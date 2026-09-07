package com.backend.travelervpn.controller

import com.backend.travelervpn.entity.VpnUser
import com.backend.travelervpn.generated.api.schema.Client
import com.backend.travelervpn.generated.api.schema.ClientTraffic
import com.backend.travelervpn.generated.api.schema.Inbound
import com.backend.travelervpn.repository.VpnUserRepository
import com.backend.travelervpn.repository.VpnUserRepositoryReactive
import com.backend.travelervpn.service.geoip.GeoIpService
import com.backend.travelervpn.service.VpnLinkExtractorService
import com.backend.travelervpn.service.xui.XUIManagerService
import com.backend.travelervpn.service.xui.XuiWebSocketData
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.*
import tools.jackson.databind.ObjectMapper
import java.security.CryptoPrimitive
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.jvm.optionals.getOrNull

data class VpnResponse(
    val status: String,
    val response: Any? = null,
    val message: String? = null
)

@RestController
@RequestMapping("/api")
class VpnController(
    private val vpnUserRepository: VpnUserRepository,
    private val vpnUserRepositoryReactive: VpnUserRepositoryReactive,
    private val xuiManagerService: XUIManagerService,
    private val vpnLinkExtractorService: VpnLinkExtractorService,
    private val messagingTemplate: SimpMessagingTemplate,
    private val objectMapper: ObjectMapper,
    private val geoIpService: GeoIpService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private var wsSession: DefaultClientWebSocketSession? = null
    private var wsFuture = CompletableDeferred<DefaultClientWebSocketSession>()

    private suspend fun createNewUser(userId: String? = null, inbounds: List<Inbound>): VpnUser {
        val expiryAt = Instant.now()
            .plus(7, ChronoUnit.DAYS)

        val trafficByte = 26843545600L

        val newUser = xuiManagerService.createClient(
            userId = userId,
            expiryTime = expiryAt.toEpochMilli(),
            totalGB = trafficByte,
            inbounds = inbounds.map { it.id }
        )
            ?: throw IllegalStateException("Error creating client")

        delay(500L)

        val connectionLinks =
            vpnLinkExtractorService.extractLinksFromSubscription(
                "${xuiManagerService.subUrl}/${newUser.subId}"
            )

        return vpnUserRepository.save(
            VpnUser(
                userId = UUID.fromString(newUser.id),
                username = Instant
                    .now()
                    .toEpochMilli()
                    .toString()
                    .plus(
                        "_${
                            newUser.id
                                .toString()
                                .replace("-", "")
                                .take(8)
                        }"
                    ),
                connectionLinks = connectionLinks,
                expiryAt = expiryAt.toEpochMilli(),
                trafficLeft = trafficByte,
                email = newUser.email,
            )
        )
    }

    @PostMapping("/user/subscription")
    suspend fun subscription(@RequestParam(required = false) userId: String?): VpnResponse {
        return try {
            var user: VpnUser? = null
            var message: String? = null
            val inbounds = xuiManagerService.getAllInbounds() ?: throw IllegalStateException("No inbounds found")

            if (userId != null && userId.isNotEmpty()) {
                user = vpnUserRepository.findById(userId).getOrNull()

                if (user == null) {
                    user = createNewUser(inbounds = inbounds)
                }
            } else {
                user = createNewUser(userId, inbounds)
                message = "Created new user"
            }

            data class SubscriptionResponse(
                val inbound: Inbound,
                val connectionLink: String
            )

            val response: MutableList<SubscriptionResponse> = mutableListOf()

            inbounds.zip(user.connectionLinks.toTypedArray()).forEach {
                response.add(
                    SubscriptionResponse(
                        it.first,
                        it.second
                    )
                )
            }

            VpnResponse(
                status = "success",
                response = response,
                message = message
            )
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Internal server error")
        }
    }

    @GetMapping("/user")
    suspend fun user(userId: String): VpnResponse {
        return try {
            val user = xuiManagerService.getClientByEmail(userId) as Client?
                ?: throw IllegalStateException("No user with id $userId")

            VpnResponse(
                status = "success",
                response = user,
            )
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Internal server error")
        }
    }

    @RequestMapping(path = ["/user/update"], method = [RequestMethod.PUT])
    suspend fun updateUser(userId: String, client: Client): VpnResponse {
        return try {
            vpnUserRepository.findById(userId).getOrNull()
                ?: throw IllegalStateException("No user with id $userId")

            xuiManagerService.updateClient(
                userId,
                client
            )



            VpnResponse("success")
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Internal server error")
        }
    }

    @GetMapping(path = ["/user/traffic"])
    suspend fun userTraffic(userId: String): VpnResponse {
        return try {
            val response = xuiManagerService.getClientTraffic(userId)

            VpnResponse(status = "success", response = response)
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Internal server error")
        }
    }

    @PostMapping(path = ["/user/inbounds/detach"])
    suspend fun detachInbounds(userId: String, inbounds: List<Int>): VpnResponse {
        return try {
            val response = xuiManagerService.detachInbounds(userId, inbounds)

            VpnResponse(status = "success", response = response)
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Internal server error")
        }
    }

    @PostMapping(path = ["/node/inbounds/attach"])
    suspend fun attachInbounds(userId: String, inbounds: List<Int>): VpnResponse {
        return try {
            val response = xuiManagerService.attachInbounds(userId, inbounds)

            VpnResponse(status = "success", response = response)
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Internal server error")
        }
    }

    @PostMapping(path = ["/user/geo"])
    suspend fun getUserLastGeo(email: String): VpnResponse {
        return try {
            val response = xuiManagerService.getClientIps(email)

            if(response.isNullOrEmpty()) VpnResponse(status = "error", message = "Client doesnt have any ip logs")
            else {
                val geo = geoIpService.lookupIp(response.last())

                if(geo === null) {
                    VpnResponse(status = "error", message = "Ip not found")
                } else {
                    VpnResponse(status = "success", response = geo)
                }
            }
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Internal server error")
        }
    }

    @GetMapping(path = ["/ws"])
    suspend fun ws(): VpnResponse {
        return try {
            xuiManagerService.login()

            wsSession = xuiManagerService.ws()
            if (wsSession !== null) {
                wsFuture.complete(wsSession!!)
            } else {
                wsFuture.completeExceptionally(Exception("No WS Session"))
            }

            VpnResponse(status = "success")
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Internal server error")
        }
    }

    @GetMapping(path = ["/ws/logout"])
    suspend fun wsLogout(): VpnResponse {
        return try {
            val response = xuiManagerService.logout()

            VpnResponse(status = "success", response = response)
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Internal server error")
        } finally {
            wsSession?.close(CloseReason(CloseReason.Codes.NORMAL, "Client disconnected"))
            wsSession = null
        }
    }

    @MessageMapping("/client/traffic")
    @SendTo("/data/client/traffic")
    fun clientTraffic() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val session = wsFuture.await()
                session.let { it ->
                    for (frame in it.incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                try {
                                    val socketData = objectMapper.readValue(frame.data, XuiWebSocketData::class.java)
                                    var clientEmail = ""
                                    log.info("Received payload: $socketData")

                                    if (socketData.type === "client_creds") {
                                        if (socketData.payload is String)
                                            clientEmail = socketData.payload
                                        else
                                            throw IllegalArgumentException("Payload must be a string")
                                    }

                                    when (socketData.type) {
                                        "client_stats" -> {
                                            try {
                                                val payload = socketData.payload
                                                for (client in payload as Array<ClientTraffic>) {
                                                    if (vpnUserRepository.findAll()
                                                            .firstOrNull
                                                            { user -> user.email == client.email }
                                                        != null
                                                    ) {
                                                        vpnUserRepositoryReactive.setTotal(
                                                            client.email,
                                                            client.total
                                                        )
                                                    }
                                                    if (client.email == clientEmail) {
                                                        messagingTemplate.convertAndSend(
                                                            "/ws/metrics/data",
                                                            client
                                                        )
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                log.error(e.message, e)
                                            }
                                        }

                                        else -> continue
                                    }
                                } catch (ex: Exception) {
                                    log.error(ex.message, ex)
                                }
                            }

                            is Frame.Close -> {
                                log.info("WebSocket connection closed")
                                messagingTemplate.convertAndSend(
                                    "/ws/metrics/data",
                                    mapOf(
                                        "type" to "closed",
                                        "code" to frame.readReason()?.code,
                                        "reason" to frame.readReason()?.message
                                    )
                                )
                            }

                            else -> {
                                log.info("Skipping frame of type ${frame.frameType.name}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                log.error("Error forwarding WebSocket data: ${e.message}", e)
                try {
                    messagingTemplate.convertAndSend(
                        "/ws/metrics/data",
                        mapOf(
                            "type" to "error",
                            "message" to (e.message ?: "Unknown error")
                        ) as Any
                    )
                } catch (_: Exception) {
                    // Connection already lost
                }
                wsSession?.close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Internal Error"))
                wsSession = null
            }
        }
    }
}
