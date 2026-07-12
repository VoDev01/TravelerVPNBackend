package com.backend.travelervpn.repository

import com.backend.travelervpn.entity.VpnUser
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.stereotype.Repository

@Repository
interface VpnUserRepository : CassandraRepository<VpnUser, String>
