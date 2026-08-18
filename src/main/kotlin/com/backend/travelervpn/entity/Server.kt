package com.backend.travelervpn.entity

import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table

@Table("servers")
data class Server(
    @PrimaryKey
    val id: Long,
    val name: String,
    val location: String,
)
