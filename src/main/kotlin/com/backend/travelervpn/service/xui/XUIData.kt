package com.backend.travelervpn.service.xui

import com.backend.travelervpn.generated.api.schema.Node
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class XuiWebSocketData<T>(
    val type: String,
    val payload: T?
)

data class TestNodeRequest(
    val scheme: Node.Scheme,
    val address: String,
    val port: Int,
    val basePath: String,
    val apiToken: String,
)

data class AddNodeRequest(
    val name: String,
    val remark: String,
    val scheme: Node.Scheme,
    val address: String,
    val basePath: String,
    val apiToken: String,
    val clearApiToken: Boolean,
    val enable: Boolean,
    val allowPrivateAddress: Boolean,
)