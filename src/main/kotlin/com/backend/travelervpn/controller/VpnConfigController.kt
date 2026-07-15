package com.backend.travelervpn.controller

import com.backend.travelervpn.config.AppProperties
import com.backend.travelervpn.entity.VpnUser
import com.backend.travelervpn.repository.VpnUserRepository
import com.backend.travelervpn.service.SingBoxManagerService
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

data class VpnConfigResponse(
    val status: String,
    val uuid: String? = null,
    val configUrl: String? = null,
    val message: String? = null
)

@RestController
@RequestMapping("/api/vpn")
class VpnConfigController(
    private val vpnUserRepository: VpnUserRepository,
    private val singBoxManagerService: SingBoxManagerService,
    private val appProperties: AppProperties
) {

    @GetMapping("/generate")
    suspend fun generateClientConfig(@RequestParam(required = false) userId: String?): VpnConfigResponse {

        if (userId != null) {
            val existingUser = vpnUserRepository.findById(userId).getOrNull()

            if (existingUser != null) {
                return VpnConfigResponse("success", existingUser.clientUuid, existingUser.configUrl)
            }
        }
        val clientUuid = UUID.randomUUID().toString()
        val vlessLink = "vless://$clientUuid@${appProperties.serverIp}:${appProperties.serverPort}" +
                "?flow=xtls-rprx-vision&security=reality&sni=${appProperties.maskDomain}" +
                "&fp=chrome&pbk=${appProperties.realityPublicKey}&sid=${appProperties.realityShortId}#Oracle-$userId"

        val newUser = VpnUser(userId = clientUuid, clientUuid = clientUuid, configUrl = vlessLink)

        vpnUserRepository.save(newUser)

        try {
            singBoxManagerService.addUserToVpn(clientUuid)
        } catch (e: Exception) {
            return VpnConfigResponse(status = "failed",
                message = "Unable to connect to a server."
            )
        } finally {
            vpnUserRepository.deleteById(clientUuid)
        }

        return VpnConfigResponse("success", clientUuid, vlessLink)
    }
}
