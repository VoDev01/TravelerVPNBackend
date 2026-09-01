package com.backend.travelervpn.service
import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.exception.AddressNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.InetAddress

// A lightweight data class to return structured data
data class GeoLocationDto(
    val country: String?,
    val city: String?,
    val latitude: Double?,
    val longitude: Double?
)

@Service
class GeoIpService(private val databaseReader: DatabaseReader) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun lookupIp(ipAddress: String): GeoLocationDto? {
        return try {
            val inetAddress = InetAddress.getByName(ipAddress)
            val response = databaseReader.city(inetAddress)

            GeoLocationDto(
                country = response.country.name,
                city = response.city.name,
                latitude = response.location.latitude,
                longitude = response.location.longitude
            )
        } catch (e: AddressNotFoundException) {
            log.warn("Address '$ipAddress' could not be found: ${e.message}")
            null
        } catch (e: Exception) {
            log.error("Could not lookup geoip", e)
            null
        }
    }
}