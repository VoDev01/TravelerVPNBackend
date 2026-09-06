package com.backend.travelervpn

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
@EnableCassandraRepositories(basePackages = ["com.backend.travelervpn.repository"])
@EnableScheduling
class TravelervpnApplication

fun main(args: Array<String>) {
    runApplication<TravelervpnApplication>(*args)
}
