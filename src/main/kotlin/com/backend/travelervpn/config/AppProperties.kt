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
    lateinit var serverIp: String
    @NotNull
    lateinit var maskDomain: String
    @NotNull
    lateinit var marzbanUsername: String
    @NotNull
    lateinit var marzbanPassword: String
    @NotNull
    lateinit var xuiToken: String
    @NotNull
    lateinit var xuiUsername: String
    @NotNull
    lateinit var xuiPassword: String
    @NotNull
    lateinit var xuiAccessUrl: String
    @NotNull
    lateinit var xuiSubUrl: String
}