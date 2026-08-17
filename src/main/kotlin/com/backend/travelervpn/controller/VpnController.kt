package com.backend.travelervpn.controller

import com.backend.travelervpn.entity.VpnUser
import com.backend.travelervpn.generated.api.schema.Client
import com.backend.travelervpn.repository.VpnUserRepository
import com.backend.travelervpn.service.VpnLinkExtractorService
import com.backend.travelervpn.service.XUIManagerService
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

data class VpnResponse(
    val status: String,
    val response: Any? = null,
    val client: Client? = null,
    val connectionLinks: List<String>? = null,
    val message: String? = null
)

@RestController
@RequestMapping("/api")
class VpnController(
    private val vpnUserRepository: VpnUserRepository,
    private val xuiManagerService: XUIManagerService,
    private val vpnLinkExtractorService: VpnLinkExtractorService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private suspend fun createNewUser(userId: String? = null): VpnUser {
        val inbounds = xuiManagerService.getAllInbounds() ?: throw IllegalStateException("No inbounds found")

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

        val connectionLinks = vpnLinkExtractorService.extractLinksFromSubscription("${xuiManagerService.subUrl}/$newUser")

        val uuid = UUID.fromString(userId) ?: UUID.randomUUID()

        return vpnUserRepository.save(
            VpnUser(
                userId = uuid,
                username = Instant
                    .now()
                    .toEpochMilli()
                    .toString()
                    .plus("_${uuid
                        .toString()
                        .replace("-", "")
                        .take(12)}"
                    ),
                connectionLinks = connectionLinks,
                expiryAt = expiryAt,
                trafficLeft = trafficByte
            )
        )
    }

    @PostMapping("/user/subscription")
    suspend fun subscription(@RequestParam(required = false) userId: String?): VpnResponse {
        return try {
            var user: VpnUser?
            var message: String? = null
            if (userId != null && userId.isNotEmpty()) {
                user = vpnUserRepository.findById(userId).getOrNull()

                if (user == null) {
                    user = createNewUser()
                }
            } else {
                user = createNewUser(userId)
                message = "Created new user"
            }

            VpnResponse(
                status = "success",
                connectionLinks = user.connectionLinks.toList(),
                message = message
            )
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Something went wrong")
        }
    }

    @GetMapping("/user")
    suspend fun user(userId: String): VpnResponse {
        return try {
            val user = xuiManagerService.getClientByEmail(userId) as Client?
                ?: throw IllegalStateException("No user with id $userId")

            VpnResponse(
                status = "success",
                client = user,
            )
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Something went wrong")
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
            VpnResponse(status = "error", message = "Something went wrong")
        }
    }

    @GetMapping(path = ["/user/traffic"])
    suspend fun userTraffic(userId: String): VpnResponse {
        return try {
            val response = xuiManagerService.getClientTraffic(userId)

            VpnResponse(status = "success", response = response)
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Something went wrong")
        }
    }

    @PostMapping(path = ["/node/test"])
    suspend fun testNode(address: String, port: Int, apiToken: String): VpnResponse {
        return try {
            val response = xuiManagerService.testNode(address, port, apiToken)

            VpnResponse(status = "success", response = response)
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Something went wrong")
        }
    }

    @PostMapping(path = ["/user/inbounds/detach"])
    suspend fun detachInbounds(userId: String, inbounds: List<Int>): VpnResponse {
        return try {
            val response = xuiManagerService.detachInbounds(userId, inbounds)

            VpnResponse(status = "success", response = response)
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Something went wrong")
        }
    }

    @PostMapping(path = ["/node/inbounds/attach"])
    suspend fun attachInbounds(userId: String, inbounds: List<Int>): VpnResponse {
        return try {
            val response = xuiManagerService.attachInbounds(userId, inbounds)

            VpnResponse(status = "success", response = response)
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Something went wrong")
        }
    }

    @GetMapping(path = ["/ws"])
    suspend fun ws(): VpnResponse {
        return try {
            xuiManagerService.login()

            val response = xuiManagerService.ws()

            VpnResponse(status = "success", response = response)
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Something went wrong")
        }
    }

    @GetMapping(path = ["/ws/logout"])
    suspend fun wsLogout(): VpnResponse {
        return try {
            val response = xuiManagerService.logout()

            VpnResponse(status = "success", response = response)
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Something went wrong")
        }
    }
}
