package com.backend.travelervpn.service.xui

import com.backend.travelervpn.config.AppProperties
import com.backend.travelervpn.generated.api.AuthenticationApi
import com.backend.travelervpn.generated.api.ClientsApi
import com.backend.travelervpn.generated.api.InboundsApi
import com.backend.travelervpn.generated.api.NodesApi
import com.backend.travelervpn.generated.api.schema.*
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
class XUIManagerService(
    private val appProperties: AppProperties,
) {
    val subUrl = appProperties.xuiSubUrl

    private val privateUrl = "${appProperties.xuiAccessUrl}"
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
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to get csrf token")
            }

            val response = authApi.postLogin(postLoginRequest = PostLoginRequest(
                username = appProperties.xuiUsername.trim(),
                password = appProperties.xuiPassword.trim(),
                twoFactorCode = "" //TODO: IMPLEMENT TWO FA
            ))

            if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )

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
                throw ResponseStatusException(HttpStatus.NOT_FOUND,"Unable to get csrf token")
            }

            val response = authApi.postLogout()

            if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )

            logger.info("Websocket logged out")
            true
        } catch (e: Exception) {
            logger.error(e.message, e)
            false
        }
    }

    suspend fun ws(): DefaultClientWebSocketSession? {
        return try {
            val isLoggedIn = login() ?: throw Exception("Invalid credentials")
            if(!isLoggedIn) throw Exception("Invalid credentials")

            val cleanUrl = Url(privateUrl)

            wsClient.webSocketSession(
                    method = HttpMethod.Get,
                    host = cleanUrl.host,
                    port = cleanUrl.port,
                    path = "${cleanUrl.encodedPath}/ws"
                ) {
                    logger.info("WebSocket connection established")

                    url.protocol = if (cleanUrl.protocol.isSecure()) URLProtocol.WSS else URLProtocol.WS
                    header(
                        HttpHeaders.Origin,
                        privateUrl.trim()
                    )
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
            null
        }
    }

    suspend fun getClientByEmail(email: String): Client? {
        return try {
            val clientsApi = ClientsApi(privateUrl)

            clientsApi.setBearerToken(appProperties.xuiToken.trim())

            val response = clientsApi.getPanelApiClientsGetEmail(email.plus("@secret.com"))
            if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )

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
            if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )

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
        expiryTime: Long? = 0L,
        totalGB: Long? = 0L,
        tgId: Long = 0L,
        inbounds: List<Int>
    ): Client? {
        return try {
            var uuid = UUID.randomUUID().toString()
            var subId = uuid.replace("-", "")

            if(userId != null) {
                uuid = userId
                subId = uuid.replace("-", "")
            }

            val client  = Client(
                id = uuid,
                email = uuid.replace("-", "").plus("@secret.com"),
                comment = "",
                enable = true,
                expiryTime = expiryTime ?: 0L,
                limitIp = 0,
                reset = 0,
                security = "auto",
                subId = subId,
                tgId = tgId,
                totalGB = totalGB ?: 0L
            )

            val newClientRequest = mapOf(
                "client" to client,
                "inboundIds" to inbounds,
            )

            val clientsApi = ClientsApi(privateUrl)

            clientsApi.setBearerToken(appProperties.xuiToken.trim())

            val response = clientsApi.postPanelApiClientsAdd(newClientRequest)

            if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )

            client
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

            val client = getClientByEmail(email)

            if(client != null) {
                val newClient = Client(
                    id = newClient.id ?: client.id,
                    email = newClient.email ?: client.email,
                    comment = newClient.comment ?: client.comment,
                    enable = newClient.enable ?: client.enable,
                    expiryTime = newClient.expiryTime ?: client.expiryTime,
                    limitIp = newClient.limitIp ?: client.limitIp,
                    reset = newClient.reset ?: client.reset,
                    security = newClient.security ?: client.security,
                    subId = newClient.subId ?: client.subId,
                    tgId = newClient.tgId ?: client.tgId,
                    totalGB = newClient.totalGB ?: client.totalGB,
                )
                val response = clientsApi.postPanelApiClientsUpdateEmail(email.plus("@secret.com"), newClient)
                if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )
            }

        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }
    
    suspend fun getAllInbounds(): List<Inbound>? {
        return try {
            val inboundsApi = InboundsApi(privateUrl)

            inboundsApi.setBearerToken(appProperties.xuiToken.trim())

            val response = inboundsApi.getPanelApiInboundsList()

            if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )

            response.body().obj
        } catch (e: Exception) {
            logger.error(e.message, e)
            null
        }
    }

    suspend fun addInbound(request: Inbound): Any? {
        return try {
            val inboundsApi = InboundsApi(privateUrl)

            inboundsApi.setBearerToken(appProperties.xuiToken.trim())

            val response = inboundsApi.postPanelApiInboundsAdd(request)

            if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )

            if(response.body().obj is Unit || response.body().obj == null)
                null
            else
                response.body().obj
        } catch (e: Exception) {
            logger.error(e.message, e)
            null
        }
    }

    suspend fun deleteInbound(inboundId: Int): Any? {
        return try {
            val inboundsApi = InboundsApi(privateUrl)

            inboundsApi.setBearerToken(appProperties.xuiToken.trim())

            val response = inboundsApi.postPanelApiInboundsDelId(inboundId)

            if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )

            if(response.body().obj is Unit || response.body().obj == null)
                null
            else
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
            if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )

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
            if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )

        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    suspend fun getClientIps(
        email: String,
    ): List<String>? {
        return try {
            val clientsApi = ClientsApi(privateUrl)

            clientsApi.setBearerToken(appProperties.xuiToken.trim())

            val response = clientsApi.postPanelApiClientsIpsEmail(email.plus("@secret.com"))

            if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )

            if(response.body().obj is Unit || response.body().obj == null)
                null
            else
                response.body().obj as List<String>?
        } catch (e: Exception) {
            logger.error(e.message, e)
            null
        }
    }

    suspend fun getClientSubLinks(subId: String): List<String>? {
        return try{
            val clientsApi = ClientsApi(privateUrl)

            clientsApi.setBearerToken(appProperties.xuiToken.trim())

            val response = clientsApi.getPanelApiClientsSubLinksSubId(subId)

            if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )

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

            if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )

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

            if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )

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

            if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    suspend fun addNode(node: AddNodeRequest): NodeView? {
        return try {
            val nodesApi = NodesApi(privateUrl)

            nodesApi.setBearerToken(appProperties.xuiToken.trim())

            val response = nodesApi.postPanelApiNodesAdd(node)

            if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )

            response.body().obj
        } catch (e: Exception) {
            logger.error(e.message, e)
            null
        }
    }

    suspend fun deleteNode(nodeId: Int): Any? {
        return try{
            val nodesApi = NodesApi(privateUrl)

            nodesApi.setBearerToken(appProperties.xuiToken.trim())

            val response = nodesApi.postPanelApiNodesDelId(nodeId)

            if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )

            response.body().obj
        } catch (e: Exception) {
            logger.error(e.message, e)
            null
        }
    }

    suspend fun testNode(request: TestNodeRequest): ProbeResultUI? {
        return try{
            val nodesApi = NodesApi(privateUrl)

            nodesApi.setBearerToken(appProperties.xuiToken.trim())

            val response = nodesApi.postPanelApiNodesTest(request)

            if(response.status != 200) throw ResponseStatusException(
                HttpStatus.valueOf(response.status),
                response.body().msg
            )

            response.body().obj
        } catch (e: Exception) {
            logger.error(e.message, e)
            null
        }
    }
}