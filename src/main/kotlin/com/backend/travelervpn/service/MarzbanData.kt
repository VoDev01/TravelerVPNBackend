package com.backend.travelervpn.service

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import java.time.LocalDateTime
import java.time.OffsetDateTime

data class Token(
    val access_token: String,
    val token_type: String = "bearer",
)

enum class UserStatus(@get:JsonValue val value: String) {
    ACTIVE("active"),
    DISABLED("disabled"),
    LIMITED("limited"),
    EXPIRED("expired"),
    ON_HOLD("on_hold")
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UserCreate(
    val username: String,
    val status: UserStatus = UserStatus.ACTIVE,
    val data_limit: Long? = 0,
    val data_limit_reset_strategy: String = "no_reset",
    val expire: Long? = 0,
    val inbounds: Inbounds,
    val proxies: Proxies,
    val next_plan: NextPlan? = NextPlan(),
    val note: String? = "",
    val sub_updated_at: String? = null,
    val sub_last_user_agent: String? = null,
    val online_at: String? = null,
    val on_hold_expire_duration: Long? = 0,
    val on_hold_timeout: String? = null,
    val auto_delete_in_days: Int? = null
)

data class ClientResponse(
    val vlessId: String,
    val connectionLinks: List<String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UserResponse(
    val username: String,
    val status: UserStatus,
    val used_traffic: Long,
    val created_at: LocalDateTime,

    val lifetime_used_traffic: Long? = 0,
    val expire: Long? = null,
    val data_limit: Long? = null,
    val data_limit_reset_strategy: String? = null,
    val note: String? = null,
    val sub_updated_at: String? = null,
    val sub_last_user_agent: String? = null,
    val online_at: String? = null,
    val on_hold_expire_duration: Long? = null,
    val on_hold_timeout: String? = null,
    val auto_delete_in_days: Int? = null,

    val inbounds: Inbounds? = null,
    val proxies: Proxies? = null,
    val next_plan: NextPlan? = null,
    val links: List<String> = emptyList(),
    val subscription_url: String? = null,
    val excluded_inbounds: Map<String, Any>? = null,
    val admin: Admin? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UserModify(
    val proxies: Proxies? = null,
    val expire: Long? = null,
    val data_limit: Long? = null,
    val data_limit_reset_strategy: String? = null,
    val inbounds: Inbounds? = null,
    val note: String? = null,
    val status: UserStatus? = null,

    val sub_updated_at: String? = null,
    val sub_last_user_agent: String? = null,
    val online_at: String? = null,
    val on_hold_expire_duration: Long? = null,
    val on_hold_timeout: String? = null,
    val auto_delete_in_days: Int? = null,
    val next_plan: NextPlanModify? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NextPlanModify(
    val data_limit: Long? = null,
    val expire: Long? = null,
    val add_remaining_traffic: Boolean? = null,
    val fire_on_either: Boolean? = null
)

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class Inbounds(
    val vless: List<String>,
    val vmess: List<String> = emptyList()
)

data class NextPlan(
    val add_remaining_traffic: Boolean = false,
    val data_limit: Long = 0,
    val expire: Long = 0,
    val fire_on_either: Boolean = true
)

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class Proxies(
    val vless: VlessConfig,
    val vmess: Map<String, Any> = emptyMap()
)

data class VlessConfig(
    val id: String
)

data class Admin(
    val username: String,
    val is_sudo: Boolean,

    val telegram_id: Long? = null,
    val discord_webhook: String? = null,
    val users_usage: Long? = null
)
