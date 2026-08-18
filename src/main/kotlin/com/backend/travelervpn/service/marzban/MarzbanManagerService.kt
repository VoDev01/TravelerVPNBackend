package com.backend.travelervpn.service.marzban

import com.backend.travelervpn.config.AppProperties
import com.backend.travelervpn.entity.VpnUser
import com.backend.travelervpn.repository.VpnUserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.jvm.optionals.getOrNull

@Service
class MarzbanManagerService(
    private val vpnUserRepository: VpnUserRepository,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val token = AtomicReference<String?>(null)

    private val webClient = WebClient.builder()
        .baseUrl("http://host.docker.internal:8000/api")
        .build()

    private suspend fun authenticate(): String {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("username", appProperties.marzbanUsername)
            add("password", appProperties.marzbanPassword)
        }

        val response = webClient
            .post()
            .uri("/admin/token")
            .bodyValue(formData)
            .retrieve()
            .awaitBody<Token>()

        token.set(response.access_token)
        return response.access_token
    }

    private suspend fun getCachedToken(): String {
        return token.get() ?: authenticate()
    }

    suspend fun addUserToVpn(): ClientResponse? {
        val datePrefix = OffsetDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyMMdd"))
        val shortUuid = UUID.randomUUID().toString()
            .replace("-", "").take(6)

        val username = "vpn_${datePrefix}_$shortUuid"

        val vlessId = UUID.randomUUID()

        val request = UserCreate(
            username = username,
            status = UserStatus.DISABLED,
            inbounds = MarzbanInbounds(
                vless = listOf("VLESS TCP REALITY")
            ),
            proxies = Proxies(
                vless = VlessConfig(id = vlessId.toString())
            )
        )

        return try {
            val token = getCachedToken()
            val response = webClient
                .post()
                .uri("/user")
                .headers { headers -> headers.setBearerAuth(token) }
                .bodyValue(request)
                .retrieve()
                .awaitBody<UserResponse>()

            val subscriptionLink = response.subscription_url ?: throw IllegalStateException(
                "Marzban API не вернул subscription_url для пользователя ${request.username}"
            )

            val connectionLinks = response.links
            if(connectionLinks.isEmpty()) {
                throw IllegalStateException(
                    "Marzban API не вернул links для пользователя ${request.username}"
                )
            }

            vpnUserRepository.save(
                VpnUser(
                    userId = vlessId,
                    username = username,
                    connectionLinks = connectionLinks.toSet()
                )
            )

            ClientResponse(
                vlessId = vlessId.toString(),
                connectionLinks = connectionLinks
            )
        } catch (ex: WebClientResponseException) {
            log.error(
                "Ошибка API Marzban при создании пользователя {}. Статус: {}, Тело ответа: {}",
                request.username, ex.statusCode, ex.responseBodyAsString, ex
            )
            null
        } catch (ex: Exception) {
            log.error("Непредвиденная ошибка при создании пользователя {}", request.username, ex)
            null
        }
    }

    suspend fun fetchUser(vlessId: String): ClientResponse? {
        return try {
            val user =
                vpnUserRepository.findById(vlessId).getOrNull()
                    ?: throw IllegalStateException("Пользователь не найден")

            val token = getCachedToken()
            val response = webClient
                .get()
                .uri("/user/${user.username}")
                .headers { headers -> headers.setBearerAuth(token) }
                .retrieve()
                .awaitBody<UserResponse>()

            ClientResponse(
                vlessId = user.userId.toString(),
                connectionLinks = response.links
            )
        } catch (ex: Exception) {
            log.error("Ошибка при попытке найти пользователя {}", vlessId, ex)
            null
        }
    }

    suspend fun renewUserSubscription(username: String, newDataLimit: Long)
            : String {
        val modifyRequest = UserModify(
            data_limit = newDataLimit,
            status = UserStatus.ACTIVE
        )

        return try {
            val token = getCachedToken()
            webClient.put()
                .uri("/user/$username")
                .bodyValue(modifyRequest)
                .headers { headers -> headers.setBearerAuth(token) }
                .retrieve()
                .awaitBody<UserResponse>()

            "Renewed"
        } catch (ex: WebClientResponseException) {
            log.error(
                "Ошибка API Marzban при попытке отключить пользователя {}. Код: {}, Ответ: {}",
                username, ex.statusCode, ex.responseBodyAsString, ex
            )
            "Something went wrong"
        } catch (ex: Exception) {
            log.error("Не удалось отключить пользователя {} из-за системной ошибки", username, ex)
            "Something went wrong"
        }
    }

    suspend fun updateUserStatus(
        username: String,
        vlessId: UUID,
        userStatus: UserStatus
    ): String {
        val statusRequest = UserModify(
            status = userStatus
        )

        return try {
            log.info("Попытка отключения пользователя {} от VPN-сервера...", username)

            val token = getCachedToken()
            val response = webClient.put()
                .uri("/user/$username")
                .bodyValue(statusRequest)
                .headers { headers -> headers.setBearerAuth(token) }
                .retrieve()
                .awaitBody<UserResponse>()

            log.info("Пользователь {} успешно переведен в статус {} на сервере Marzban.", username, response.status)

            updateUserStatusInCassandra(vlessId)

            userStatus.value
        } catch (ex: WebClientResponseException) {
            log.error(
                "Ошибка API Marzban при попытке отключить пользователя {}. Код: {}, Ответ: {}",
                username, ex.statusCode, ex.responseBodyAsString, ex
            )
            "Something went wrong"
        } catch (ex: Exception) {
            log.error("Не удалось отключить пользователя {} из-за системной ошибки", username, ex)
            "Something went wrong"
        }
    }

    private suspend fun updateUserStatusInCassandra(vlessId: UUID) {
        try {
            val vpnUserOptional = vpnUserRepository.findById(vlessId.toString())

            if (vpnUserOptional.isPresent) {
                val vpnUser = vpnUserOptional.get()

                val updatedUser = vpnUser.copy(status = UserStatus.DISABLED)

                vpnUserRepository.save(updatedUser)
                log.info("Статус пользователя с ID {} успешно обновлен в Cassandra.", vlessId)
            } else {
                log.warn("Пользователь с ID {} не найден в базе данных Cassandra для обновления статуса.", vlessId)
            }
        } catch (ex: Exception) {
            log.error("Не удалось обновить статус пользователя {} в Cassandra", vlessId, ex)
        }
    }
}