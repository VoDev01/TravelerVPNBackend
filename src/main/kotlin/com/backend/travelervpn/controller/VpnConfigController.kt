package com.backend.travelervpn.controller

import com.backend.travelervpn.entity.VpnUser
import com.backend.travelervpn.repository.VpnUserRepository
import com.backend.travelervpn.service.SingBoxManagerService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class VpnConfigResponse(val status: String, val uuid: String, val configUrl: String)

@RestController
@RequestMapping("/api/vpn")
class VpnConfigController(
    private val vpnUserRepository: VpnUserRepository,
    private val singBoxManagerService: SingBoxManagerService
) {
    private val oracleServerIp = "143.244.11.22"
    private val serverPort = 443
    private val realityPublicKey = "Y2hvb29zZV9hX3JlYWxfcHVibGljX2tleV9mcm9tX3NpbmdfYm94"
    private val realityShortId = "0123456789abcdef"
    private val maskDomain = "://microsoft.com"

    @GetMapping("/generate")
    suspend fun generateClientConfig(@RequestParam(required = false) userId: String?): VpnConfigResponse {

        if (userId != null) {
            val existingUser = vpnUserRepository.findById(userId)

            if (existingUser != null) {
                return VpnConfigResponse("success", existingUser.clientUuid, existingUser.configUrl)
            }
        }
        val clientUuid = UUID.randomUUID().toString()
        val vlessLink = "vless://$clientUuid@$oracleServerIp:$serverPort" +
                "?flow=xtls-rprx-vision&security=reality&sni=$maskDomain" +
                "&fp=chrome&pbk=$realityPublicKey&sid=$realityShortId#Oracle-$userId"

        val newUser = VpnUser(userId = clientUuid, clientUuid = clientUuid, configUrl = vlessLink)

        vpnUserRepository.save(newUser)
        singBoxManagerService.addUserToVpn(clientUuid)

        return VpnConfigResponse("success", clientUuid, vlessLink)
    }
}
