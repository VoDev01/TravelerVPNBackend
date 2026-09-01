package com.backend.travelervpn.repository

import com.backend.travelervpn.entity.Inbound
import org.springframework.data.cassandra.repository.CassandraRepository

interface InboundRepository : CassandraRepository<Inbound, Long>