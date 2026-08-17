package com.backend.travelervpn.service

import com.backend.travelervpn.config.AppProperties
import com.backend.travelervpn.generated.api.AuthenticationApi
import com.backend.travelervpn.generated.api.ClientsApi
import com.backend.travelervpn.generated.api.InboundsApi
import com.backend.travelervpn.generated.api.NodesApi
import com.backend.travelervpn.generated.api.WebSocketApi
import com.backend.travelervpn.generated.api.schema.Client
import com.backend.travelervpn.generated.api.schema.ClientTraffic
import com.backend.travelervpn.generated.api.schema.Inbound
import com.backend.travelervpn.generated.api.schema.PostLoginRequest
import com.backend.travelervpn.generated.api.schema.ProbeResultUI
import org.apache.hc.core5.http.HttpException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class XUIManagerService(
    private val appProperties: AppProperties
) {
    val subUrl = "http://host.docker.internal:2096/sub"
    private val privateUrl = "http://host.docker.internal:52010/${appProperties.xuiSecretPath}"

    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun login(): String? {
        return try {
            val authApi = AuthenticationApi(privateUrl)

            authApi.setBearerToken(appProperties.xuiToken.trim())

            val response = authApi.postLogin(postLoginRequest = PostLoginRequest(
                username = appProperties.xuiUsername,
                password = appProperties.xuiPassword,
                twoFactorCode = "" //TODO: IMPLEMENT TWO FA
            ))

            if(response.status != 200) throw HttpException(response.body().msg)

            if(response.body().obj is Unit || response.body().obj == null)
                null
            else
                response.body().obj as String?
        } catch (e: Exception) {
            logger.error(e.message, e)
            null
        }
    }

    suspend fun logout(): Boolean {
        return try {
            val authApi = AuthenticationApi(privateUrl)

            val csrf = csrf() ?: throw Exception("Unable to get csrf")

            authApi.setApiKey(csrf)

            val response = authApi.postLogout()

            if(response.status != 200) throw HttpException(response.body().msg)

            true
        } catch (e: Exception) {
            logger.error(e.message, e)
            false
        }
    }

    suspend fun csrf(): String? {
        return try {
            val authApi = AuthenticationApi(privateUrl)

            authApi.setBearerToken(appProperties.xuiToken.trim())

            val response = authApi.getCsrfToken()

            if(response.status != 200) throw HttpException(response.body().msg)

            if(response.body().obj is Unit || response.body().obj == null)
                null
            else
                response.body().obj as String?
        } catch (e: Exception) {
            logger.error(e.message, e)
            null
        }
    }

    suspend fun ws(): Any? {
        return try {
            val wsApi = WebSocketApi(privateUrl)

            val sessionToken = login() ?: throw Exception("Invalid credentials")

            wsApi.setApiKey(sessionToken)

            val response = wsApi.getWs()

            if(response.status != 101) throw HttpException(response.body().msg)

            if(response.body().obj is Unit || response.body().obj == null)
                null
            else
                response.body().obj
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