package com.backend.travelervpn.entity

import org.springframework.data.cassandra.core.mapping.CassandraType
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table

@Table("vpn_users")
data class VpnUser(
    @PrimaryKey
    @CassandraType(type = CassandraType.Name.UUID)
    val userId: String,
    val clientUuid: String,
    val configUrl: String
)
