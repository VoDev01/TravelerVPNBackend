package com.backend.travelervpn

import io.github.cdimascio.dotenv.Dotenv
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
    val dotenv = Dotenv.configure()
        .directory("/run/secrets")
        .filename("app_secrets")
        .ignoreIfMissing()
        .load()

    dotenv.entries().forEach { entry ->
        System.setProperty(entry.key, entry.value)
    }

    runApplication<TravelervpnApplication>(*args)
}
