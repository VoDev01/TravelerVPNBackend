package com.backend.travelervpn.config

import com.maxmind.db.CHMCache
import com.maxmind.geoip2.DatabaseReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import java.io.InputStream

@Configuration
class GeoIpConfig(private val resourceLoader: ResourceLoader) {

    @Bean
    fun databaseReader(): DatabaseReader {
        val resource = resourceLoader.getResource("classpath:maxmind/GeoLite2-City.mmdb")
        val inputStream: InputStream = resource.inputStream

        return DatabaseReader.Builder(inputStream)
            .withCache(CHMCache()) // Enables concurrent caching for faster lookups
            .build()
    }
}