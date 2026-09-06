package com.backend.travelervpn.config

import com.maxmind.db.CHMCache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import java.io.File

@Configuration
class GeoIpConfig(private val resourceLoader: ResourceLoader) {

    @Bean
    fun geoIpCache(): CHMCache = CHMCache()

    fun getDefaultDatabaseFile(): File {
        val resource = resourceLoader.getResource("classpath:maxmind/GeoLite2-City.mmdb")
        return resource.file
    }
}
