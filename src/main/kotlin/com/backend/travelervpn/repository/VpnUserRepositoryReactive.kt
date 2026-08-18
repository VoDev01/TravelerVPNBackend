package com.backend.travelervpn.repository

import com.backend.travelervpn.entity.VpnUser
import kotlinx.coroutines.flow.Flow
import org.springframework.data.cassandra.repository.Query
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface VpnUserRepositoryReactive : ReactiveCassandraRepository<VpnUser, String> {
    @Query("SELECT traffic_left FROM vpn_user WHERE email = ?0")
    fun total(email: String): Flow<Long?>

    @Query("UPDATE vpn_user SET traffic_left = ?1 WHERE email = ?0")
    suspend fun setTotal(email: String, trafficLeft: Long): Boolean
}