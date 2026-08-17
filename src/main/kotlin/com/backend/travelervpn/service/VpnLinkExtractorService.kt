package com.backend.travelervpn.service

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.util.Base64
import java.nio.charset.StandardCharsets
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VpnLinkExtractorService {
    private val logger = LoggerFactory.getLogger(VpnLinkExtractorService::class.java)
    private val httpClient = HttpClient()

    suspend fun extractLinksFromSubscription(subscriptionUrl: String): Set<String> {
        return try {
            val httpResponse: HttpResponse = httpClient.get(subscriptionUrl)

            if (httpResponse.status.value != 200) {
                logger.error("3x-ui error: ${httpResponse.status}")
                return emptySet()
            }

            val base64ResponseBody = httpResponse.bodyAsText().trim()
            if (base64ResponseBody.isEmpty()) return emptySet()

            val decodedBytes = Base64.getDecoder().decode(base64ResponseBody)
            val clearTextConfigs = String(decodedBytes, StandardCharsets.UTF_8)

            val connectionLinks = clearTextConfigs.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            logger.info("Successfully extracted connection links: ${connectionLinks.size}")
            connectionLinks.toSet()

        } catch (e: Exception) {
            logger.error("Couldn't extract connection links: ${e.message}", e)
            emptySet()
        }
    }
}
