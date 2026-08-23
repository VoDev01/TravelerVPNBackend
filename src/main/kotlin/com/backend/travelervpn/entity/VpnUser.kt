package com.backend.travelervpn.entity

import com.backend.travelervpn.service.marzban.UserStatus
import org.springframework.data.cassandra.core.mapping.CassandraType
import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.time.Instant
import java.util.UUID

enum class UserPlan(var value: String) {
    FREE("FREE"),
    PREMIUM("PREMIUM"),
}

@Table("vpn_users")
data class VpnUser(
    @PrimaryKey
    @CassandraType(type = CassandraType.Name.UUID)
    @Column("user_id")
    val userId: UUID,
    val username: String,
    val email: String,
    @Column("connection_links")
    val connectionLinks: Set<String> = setOf(),
    @Column("plan")
    val plan: UserPlan = UserPlan.FREE,
    val status: UserStatus = UserStatus.ACTIVE,
    @Column("last_payment_at")
    val lastPaymentAt: Long? = null,
    @Column("expiry_at")
    val expiryAt: Long? = null,
    @Column("traffic_left")
    val trafficLeft: Long? = null,
)
