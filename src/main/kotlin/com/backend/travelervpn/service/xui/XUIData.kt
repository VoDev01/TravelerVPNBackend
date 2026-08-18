package com.backend.travelervpn.service.xui

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class XuiClientStatsPayload(
    val id: String,
    val subId: String,
    val email: String,
    val up: Long,
    val down: Long,
    val total: Long,
    val expiryTime: Long,
    val lastOnline: Long
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class XuiWebSocketData<T>(
    val type: String,
    val payload: T?
)