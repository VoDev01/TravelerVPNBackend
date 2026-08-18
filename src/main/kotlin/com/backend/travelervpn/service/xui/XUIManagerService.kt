package com.backend.travelervpn.service.xui

import com.backend.travelervpn.config.AppProperties
import com.backend.travelervpn.generated.api.AuthenticationApi
import com.backend.travelervpn.generated.api.ClientsApi
import com.backend.travelervpn.generated.api.InboundsApi
import com.backend.travelervpn.generated.api.NodesApi
import com.backend.travelervpn.generated.api.schema.Client
import com.backend.travelervpn.generated.api.schema.ClientTraffic
import com.backend.travelervpn.generated.api.schema.Inbound
import com.backend.travelervpn.generated.api.schema.PostLoginRequest
import com.backend.travelervpn.generated.api.schema.ProbeResultUI
import com.backend.travelervpn.repository.VpnUserRepositoryReactive
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.isSecure
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.apache.hc.core5.http.HttpException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class XUIManagerService(
    private val appProperties: AppProperties,
    private val objectMapper: ObjectMapper,
    private val vpnUserRepositoryReactive: VpnUserRepositoryReactive
) {
    val subUrl = "http://host.docker.internal:2096/sub"

    private val privateUrl = "http://host.docker.internal:56832/${appProperties.xuiSecretPath}"
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val sharedCookiesStorage = AcceptAllCookiesStorage()
    private val wsClient = HttpClient {
        install(WebSockets)
        install(HttpCookies) {
            storage = sharedCookiesStorage
        }
        defaultRequest {
            if (url.protocol == URLProtocol.HTTP) url.protocol = URLProtocol.WS
            if (url.protocol == URLProtocol.HTTPS) url.protocol = URLProtocol.WSS
        }
    }
    private var wsJob: Job? = null

    suspend fun login(): Boolean? {
        return try {
            var csrfToken: String? = null

            val authApi = AuthenticationApi(
                baseUrl = privateUrl,
                httpClientConfig = {
                    it.install(HttpCookies) {
                        storage = sharedCookiesStorage
                    }
                    it.defaultRequest {
                        csrfToken?.let { token ->
                            header("X-CSRF-Token", token)
                        }
                    }
                }
            )

            authApi.setBearerToken(appProperties.xuiToken.trim())

            val csrfResponse = authApi.getCsrfToken()

            csrfToken = (csrfResponse.body().obj
                ?: csrfResponse.headers["X-CSRF-Token"]
                ?: csrfResponse.body().msg) as String?

            if (csrfToken.isNullOrBlank()) {
                throw HttpException("Unable to get csrf token")
            }

            val response = authApi.postLogin(postLoginRequest = PostLoginRequest(
                username = appProperties.xuiUsername.trim(),
                password = appProperties.xuiPassword.trim(),
                twoFactorCode = "" //TODO: IMPLEMENT TWO FA
            ))

            if(response.status != 200) throw HttpException(response.body().msg)

            response.body().success
        } catch (e: Exception) {
            logger.error(e.message, e)
            null
        }
    }

    suspend fun logout(): Boolean {
        return try {
            var csrfToken: String? = null

            val authApi = AuthenticationApi(
                baseUrl = privateUrl,
                httpClientConfig = {
                    it.install(HttpCookies) {
                        storage = sharedCookiesStorage
                    }
                    it.defaultRequest {
                        csrfToken?.let { token ->
                            header("X-CSRF-Token", token)
                        }
                    }
                }
            )

            val csrfResponse = authApi.getCsrfToken()

            csrfToken = (csrfResponse.body().obj
                ?: csrfResponse.headers["X-CSRF-Token"]
                ?: csrfResponse.body().msg) as String?

            if (csrfToken.isNullOrBlank()) {
                throw HttpException("Unable to get csrf token")
            }

            val response = authApi.postLogout()

            if(response.status != 200) throw HttpException(response.body().msg)

            logger.info("Logged out")
            true
        } catch (e: Exception) {
            logger.error(e.message, e)
            false
        } finally {
            wsJob?.cancel()
            wsJob = null
        }
    }

    suspend fun ws(): DefaultClientWebSocketSession? {
        var wsSession: DefaultClientWebSocketSession? = null
        return try {
            val isLoggedIn = login() ?: throw Exception("Invalid credentials")
            if(!isLoggedIn) throw Exception("Invalid credentials")

            val cleanUrl = Url(privateUrl)

            wsJob = CoroutineScope(Dispatchers.IO).launch {
                wsClient.webSocket(
                    method = HttpMethod.Get,
                    host = cleanUrl.host,
                    port = cleanUrl.port,
                    path = "${cleanUrl.encodedPath}/ws",
                    request = {
                        url.protocol = if (cleanUrl.protocol.isSecure()) URLProtocol.WSS else URLProtocol.WS
                        header(
                            HttpHeaders.Origin,
                            privateUrl.trim()
                        )
                    }
                ) {
                    logger.info("WebSocket connection established")

                    wsSession = this

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val data = objectMapper.readValue(frame.data, XuiWebSocketData::class.java)
                            when(data.type) {
                                "client_traffic" -> {
                                    val payload = data.payload
                                    if (payload is XuiClientStatsPayload) {
                                        try {
                                            vpnUserRepositoryReactive.setTotal(payload.email, payload.total)
                                        } catch (e: Exception) {
                                            logger.error(e.message)
                                        }
                                    }
                                }
                                else -> continue
                            }
                        }
                    }
                }
            }

            wsSession
        } catch (e: Exception) {
            logger.error(e.message, e)
            wsSession?.close(reason = CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Closing session due to error"))
            wsJob?.cancel()
            wsJob = null
            null
        }
    }

    suspend fun getClientByEmail(email: String): Client? {
        return try {
            val clientsApi = ClientsApi(privateUrl)

            clientsApi.setBearerToken(appProperties.xuiToken.trim())

            val response = clientsApi.getPanelApiClientsGetEmail(email.plus("@secret.com"))
            if(response.status != 200) throw HttpException(response.body().msg)

            if(response.body().obj is Unit || response.body().obj == null)
                null
            else
                response.body().obj as Client?
        } catch (e: Exception) {
            logger.error(e.message, e)
            null
        }
    }

    suspend fun getClientByTgId(tgId: Int): Client? {
        return try {
            val clientsApi = ClientsApi(privateUrl)

            clientsApi.setBearerToken(appProperties.xuiToken.trim())

            val response = clientsApi.getPanelApiClientsGetTgIdTgId(tgId)
            if(response.status != 200) throw HttpException(response.body().msg)

            if(response.body().obj is Unit || response.body().obj == null)
                null
            else
                response.body().obj as Client?
        } catch (e: Exception) {
            logger.error(e.message, e)
            null
        }
    }

    suspend fun createClient(
        userId: String? = null,
        expiryTime: Long = 0L,
        totalGB: Long = 0L,
        tgId: Long = 0L,
        inbounds: List<Int>
    ): String? {
        return try {
            var uuid = UUID.randomUUID().toString()
            var subId = uuid.replace("-", "").take(16)

            if(userId != null) {
                uuid = userId
                subId = uuid.replace("-", "").take(16)
            }

            val client = Client(
                id = uuid,
                email = uuid.plus("@secret.com"),
                comment = "",
                enable = true,
                expiryTime = expiryTime,
                limitIp = 0,
                reset = 0,
                security = "auto",
                subId = subId,
                tgId = tgId,
                totalGB = totalGB
            )

            val newClientRequest = mapOf(
                "client" to client,
                "inboundIds" to inbounds,
            )

            val clientsApi = ClientsApi(privateUrl)

            clientsApi.setBearerToken(appProperties.xuiToken.trim())

            val response = clientsApi.postPanelApiClientsAdd(newClientRequest)

            if(response.status != 200) throw HttpException(response.body().msg)

            subId
        } catch (e: Exception) {
            logger.error(e.message, e)
            null
        }
    }

    suspend fun updateClient(
        email: String,
        newClient: Client,
    ) {
        try {
            val clientsApi = ClientsApi(privateUrl)

            clientsApi.setBearerToken(appProperties.xuiToken.trim())

            val response = clientsApi.postPanelApiClientsUpdateEmail(email.plus("@secret.com"), newClient)
            if(response.status != 200) throw HttpException(response.body().msg)

        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }
    
    suspend fun getAllInbounds(): List<Inbound>? {
        return try {
            val inboundsApi = InboundsApi(privateUrl)

            inboundsApi.setBearerToken(appProperties.xuiToken.trim())

            val response = inboundsApi.getPanelApiInboundsList()

            if(response.status != 200) throw HttpException(response.body().msg)

            response.body().obj
        } catch (e: Exception) {
            logger.error(e.message, e)
            null
        }
    }

    suspend fun attachInbounds(
        email: String,
        inboundIds: List<Int>,
    ) {
        try {
            val clientsApi = ClientsApi(privateUrl)

            clientsApi.setBearerToken(appProperties.xuiToken.trim())

            val response = clientsApi.postPanelApiClientsEmailAttach(email.plus("@secret.com"), inboundIds)
            if(response.status != 200) throw HttpException(response.body().msg)

        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    suspend fun detachInbounds(
        email: String,
        inboundIds: List<Int>,
    ) {
        try {
            val clientsApi = ClientsApi(privateUrl)

            clientsApi.setBearerToken(appProperties.xuiToken.trim())

            val response = clientsApi.postPanelApiClientsEmailDetach(email.plus("@secret.com"), inboundIds)
            if(response.status != 200) throw HttpException(response.body().msg)

        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    suspend fun getClientSubLinks(subId: String): List<String>? {
        return try{
            val clientsApi = ClientsApi(privateUrl)

            clientsApi.setBearerToken(appProperties.xuiToken.trim())

            val response = clientsApi.getPanelApiClientsSubLinksSubId(subId)

            if(response.status != 200) throw HttpException(response.body().msg)

            if(response.body().obj is Unit || response.body().obj == null)
                null
            else
                response.body().obj as List<String>?
        } catch (e: Exception) {
            logger.error(e.message, e)
            null
        }
    }

    suspend fun getClientLinks(email: String): List<String>? {
        return try{
            val clientsApi = ClientsApi(privateUrl)

            clientsApi.setBearerToken(appProperties.xuiToken.trim())

            val response = clientsApi.getPanelApiClientsLinksEmail(email.plus("@secret.com"))

            if(response.status != 200) throw HttpException(response.body().msg)

            if(response.body().obj is Unit || response.body().obj == null)
                null
            else
                response.body().obj as List<String>?
        } catch (e: Exception) {
            logger.error(e.message, e)
            null
        }
    }

    suspend fun getClientTraffic(email: String): ClientTraffic? {
        return try{
            val clientsApi = ClientsApi(privateUrl)

            clientsApi.setBearerToken(appProperties.xuiToken.trim())

            val response = clientsApi.getPanelApiClientsTrafficEmail(email.plus("@secret.com"))

            if(response.status != 200) throw HttpException(response.body().msg)

            response.body().obj
        } catch (e: Exception) {
            logger.error(e.message, e)
            null
        }
    }

    suspend fun resetClientTraffic(email: String) {
        try{
            val clientsApi = ClientsApi(privateUrl)

            clientsApi.setBearerToken(appProperties.xuiToken.trim())

            val response = clientsApi.postPanelApiClientsResetTrafficEmail(email.plus("@secret.com"))

            if(response.status != 200) throw HttpException(response.body().msg)
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    suspend fun testNode(address: String, port: Int, apiToken: String): ProbeResultUI? {
        return try{
            val nodesApi = NodesApi(privateUrl)

            nodesApi.setBearerToken(appProperties.xuiToken.trim())

            data class NodeTestRequest(
                val address: String,
                val port: Int,
                val basePath: String,
                val scheme: String,
                val apiToken: String,
            )

            val testRequest = NodeTestRequest(
                address = address,
                port = port,
                basePath = "/",
                scheme = "https",
                apiToken = apiToken
            )

            val response = nodesApi.postPanelApiNodesTest(testRequest)

            if(response.status != 200) throw HttpException(response.body().msg)

            response.body().obj
        } catch (e: Exception) {
            logger.error(e.message, e)
            null
        }
    }
}