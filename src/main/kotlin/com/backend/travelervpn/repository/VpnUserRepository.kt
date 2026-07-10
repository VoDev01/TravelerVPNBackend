package com.backend.travelervpn.repository

import com.backend.travelervpn.entity.VpnUser
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface VpnUserRepository : CoroutineCrudRepository<VpnUser, String>
