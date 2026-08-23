package com.backend.travelervpn.service.xui

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class XuiWebSocketData<T>(
    val type: String,
    val payload: T?
)