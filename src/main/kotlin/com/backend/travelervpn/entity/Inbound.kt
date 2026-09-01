package com.backend.travelervpn.entity

import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table

@Table("inbounds")
data class Inbound (
    @PrimaryKey
    val id: Long,
    val name: String,
    val server: Int,
)
