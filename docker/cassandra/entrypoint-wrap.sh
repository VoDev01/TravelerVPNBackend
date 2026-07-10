#!/usr/bin/env bash

echo "Waiting for Cassandra..."
while ! nc -z localhost 9042; do
  sleep 2
done

echo "Cassandra is ready. Creating keyspace..."
cqlsh localhost -e "CREATE KEYSPACE IF NOT EXISTS vpn_keyspace WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'};"

exec "$@"