package com.backend.travelervpn.controller

import com.backend.travelervpn.config.AppProperties
import com.backend.travelervpn.entity.VpnUser
import com.backend.travelervpn.repository.VpnUserRepository
import com.backend.travelervpn.service.ClientResponse
import com.backend.travelervpn.service.MarzbanManagerService
import com.backend.travelervpn.service.UserStatus
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import kotlin.jvm.optionals.getOrNull

data class VpnResponse(
    val status: String,
    val uuid: String? = null,
    val connectionLinks: List<String>? = null,
    val message: String? = null
)

@RestController
@RequestMapping("/api")
class VpnController(
    private val vpnUserRepository: VpnUserRepository,
    private val marzbanManagerService: MarzbanManagerService,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/vpn/connect")
    suspend fun connect(userId: String) {
        //TODO: Implement connection to a marzban node
    }

    @PostMapping("/user/connection_links")
    suspend fun connectionLinks(@RequestParam(required = false) userId: String?): VpnResponse {
        return try {
            var user: VpnUser?
            if (userId != null && userId.isNotEmpty()) {
                user = vpnUserRepository.findById(userId).getOrNull()

                if (user == null) {
                    val response = marzbanManagerService.addUserToVpn()
                        ?: throw IllegalStateException("No user with id $userId")

                    user = vpnUserRepository.findById(response.vlessId).get()
                }

                VpnResponse(
                    status = "success",
                    uuid = user.userId.toString(),
                    connectionLinks = user.connectionLinks.toList()
                )
            } else {
                val response = marzbanManagerService.addUserToVpn()
                    ?: throw IllegalStateException("No user with id $userId")

                user = vpnUserRepository
                    .findById(response.vlessId)
                    .get()

                VpnResponse(
                    "success",
                    user.userId.toString(),
                    user.connectionLinks.toList()
                )
            }
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Something went wrong")
        }
    }

    @GetMapping("/user")
    suspend fun user(userId: String): VpnResponse {
        return try {
            val user = marzbanManagerService.fetchUser(userId)
                ?: throw IllegalStateException("No user with id $userId")

            VpnResponse(
                "success",
                user.vlessId,
                user.connectionLinks.toList()
            )
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Something went wrong")
        }
    }

    @RequestMapping(path = ["/user/update_status"], method = [RequestMethod.PUT])
    suspend fun updateStatus(userId: String, status: UserStatus): VpnResponse {
        return try {
            val user = vpnUserRepository.findById(userId).getOrNull()
                ?: throw IllegalStateException("No user with id $userId")

            marzbanManagerService.updateUserStatus(
                user.username,
                user.userId,
                status
            )

            VpnResponse("success")
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            VpnResponse(status = "error", message = "Something went wrong")
        }
    }
}
