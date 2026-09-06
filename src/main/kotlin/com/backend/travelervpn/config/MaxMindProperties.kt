package com.backend.travelervpn.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "maxmind")
class MaxMindProperties {
    var downloadUrls: List<String> = ArrayList()
    var dbPath: String = "./data/GeoLite2-City.mmdb"
    var downloadCron: String = "0 0 3 * * MON"
}