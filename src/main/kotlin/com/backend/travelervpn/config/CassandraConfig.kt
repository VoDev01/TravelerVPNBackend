/*package com.backend.travelervpn.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration
import org.springframework.data.cassandra.config.SchemaAction
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification

@Configuration
class CassandraConfig : AbstractCassandraConfiguration() {

    override fun getKeyspaceName(): String = "vpn_keyspace"

    override fun getSchemaAction(): SchemaAction = SchemaAction.CREATE_IF_NOT_EXISTS

    override fun getContactPoints(): String = "cassandra"

    override fun getLocalDataCenter(): String = "datacenter1"

    override fun getKeyspaceCreations(): List<CreateKeyspaceSpecification> {
        val specification = CreateKeyspaceSpecification.createKeyspace("vpn_keyspace")
            .ifNotExists()
            .withSimpleReplication(3)
        return listOf(specification)
    }
}
*/