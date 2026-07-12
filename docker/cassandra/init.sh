#!/bin/bash
until cqlsh cassandra -e "describe keyspaces"; do
  echo "Waiting for Cassandra..."
  sleep 5
done
cqlsh cassandra -e "CREATE KEYSPACE IF NOT EXISTS traveler_vpn WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '3'};"
exec "$@"