package com.backend.travelervpn.config

import org.jetbrains.annotations.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated

@Configuration
@ConfigurationProperties(prefix = "spring")
@Validated
class AppProperties {
    @NotNull
    lateinit var realityPublicKey: String
    @NotNull
    lateinit var realityShortId: String
    @NotNull
    lateinit var serverIp: String
    @NotNull
    lateinit var maskDomain: String
    var serverPort = 443
}