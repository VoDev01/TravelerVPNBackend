package com.backend.travelervpn.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity

@Service
class SingBoxManagerService {

    private val webClient = WebClient.builder()
        .baseUrl("http://143.244.11.22:9090")
        .defaultHeader("Authorization", "Bearer TOK_EN")
        .build()

    suspend fun addUserToVpn(uuid: String) {
        val requestBody = mapOf(
            "name" to "user-$uuid",
            "uuid" to uuid,
            "alterId" to 0
        )

        webClient.post()
            .uri("/proxies/vless-inbound/users")
            .bodyValue(requestBody)
            .retrieve()
            .awaitBodilessEntity() // Асинхронное ожидание ответа без блокировки потока
    }
}
