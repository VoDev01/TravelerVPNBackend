package com.backend.travelervpn

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories

@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
@EnableCassandraRepositories(basePackages = ["com.backend.travelervpn.repository"])
class TravelervpnApplication

fun main(args: Array<String>) {
    runApplication<TravelervpnApplication>(*args)
}
