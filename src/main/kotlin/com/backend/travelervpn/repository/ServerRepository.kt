package com.backend.travelervpn.repository

import com.backend.travelervpn.entity.Server
import org.springframework.data.cassandra.repository.CassandraRepository

interface ServerRepository : CassandraRepository<Server, Long>