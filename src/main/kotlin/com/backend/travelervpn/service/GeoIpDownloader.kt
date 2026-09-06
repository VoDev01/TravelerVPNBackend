package com.backend.travelervpn.job

import com.backend.travelervpn.config.GeoIpConfig
import com.backend.travelervpn.config.MaxMindProperties
import com.backend.travelervpn.service.GeoIpService
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.FileOutputStream
import java.net.URI

@Service
class GeoDatabaseDownloader(
    private val geoIpService: GeoIpService,
    private val geoIpConfig: GeoIpConfig,
    private val maxMindProperties: MaxMindProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate(SimpleClientHttpRequestFactory().apply {
        setConnectTimeout(5000)
        setReadTimeout(10000)
    })

    @PostConstruct
    fun init() {
        val existingDb = File(maxMindProperties.dbPath)

        if (existingDb.exists()) {
            runCatching {
                log.info("Loading existing GeoLite2 database from disk...")
                geoIpService.updateDatabase(existingDb)
            }.onFailure {
                log.warn("Disk database file is corrupted, trying backup options...")
                loadBackupOrDownload()
            }
        } else {
            loadBackupOrDownload()
        }
    }

    private fun loadBackupOrDownload() {
        runCatching {
            log.info("Attempting to load default database from classpath...")
            val defaultFile = geoIpConfig.getDefaultDatabaseFile()
            geoIpService.updateDatabase(defaultFile)
            log.info("Successfully initialized with default classpath database.")
        }.onFailure {
            log.warn("Could not load default database from classpath. Starting immediate download...")
        }

        CoroutineScope(Dispatchers.IO).launch {
            log.info("Loading GeoLite2 database remotely...")
            downloadAndExtract()
        }
    }

    @Scheduled(cron = "#{@maxMindProperties.downloadCron}")
    fun downloadAndExtract() {
        if (maxMindProperties.downloadUrls.isEmpty()) {
            log.error("Cannot schedule GeoIp downloader: no urls specified.")
            return
        }

        val targetFile = File(maxMindProperties.dbPath).apply { parentFile?.mkdirs() }

        for (url in maxMindProperties.downloadUrls) {
            val success = runCatching {
                log.info("Downloading GeoLite2 database from: $url")
                val fileBytes = restTemplate.getForObject(URI.create(url), ByteArray::class.java)
                    ?: throw IllegalStateException("Empty response")

                FileOutputStream(targetFile).use { fos -> fos.write(fileBytes) }

                geoIpService.updateDatabase(targetFile)
                log.info("GeoLite2 database successfully updated from: $url")
                true
            }.onFailure { e ->
                log.warn("Failed to download from $url: ${e.message}")
            }.getOrDefault(false)

            if (success) return
        }
        log.error("CRITICAL: All configured GeoLite2 download URLs failed!")
    }
}
