package com.backend.travelervpn.service.geoip

import com.maxmind.geoip2.DatabaseReader
import com.maxmind.db.CHMCache
import com.maxmind.geoip2.exception.AddressNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicReference

// A lightweight data class to return structured data
data class GeoLocationDto(
    val country: String?,
    val city: String?,
    val latitude: Double?,
    val longitude: Double?
)

@Service
class GeoIpService(
    private val geoIpCache: CHMCache
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val databaseReaderRef = AtomicReference<DatabaseReader?>()

    fun updateDatabase(databaseFile: File) {
        runCatching {
            val newReader = DatabaseReader.Builder(databaseFile)
                .withCache(geoIpCache)
                .build()

            val oldReader = databaseReaderRef.getAndSet(newReader)
            oldReader?.close()
        }.onFailure { e ->
            log.error("Failed to load new database reader from file: ${databaseFile.absolutePath}", e)
            throw e
        }
    }

    fun lookupIp(ipAddress: String): GeoLocationDto? {
        val reader = databaseReaderRef.get() ?: return null

        return try {
            val inetAddress = InetAddress.getByName(ipAddress)
            val response = reader.city(inetAddress)

            GeoLocationDto(
                country = response.country.name ?: "Unknown",
                city = response.city.name ?: "Unknown",
                latitude = response.location.latitude,
                longitude = response.location.longitude
            )
        } catch (e: AddressNotFoundException) {
            log.warn("Address '$ipAddress' could not be found")
            null
        } catch (e: Exception) {
            log.error("Could not lookup geoip for $ipAddress", e)
            null
        }
    }
}