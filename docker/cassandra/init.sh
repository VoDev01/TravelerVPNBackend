#!/bin/bash

set -euo pipefail

SECRET_FILE="/run/secrets/app_secrets"

if [ -f "$SECRET_FILE" ]; then
  set -a
  source "$SECRET_FILE"
  set +a

  export CASSANDRA_USER=$(echo -n "$CASSANDRA_USER" | tr -d '\r\n[:space:]')
  export CASSANDRA_PASSWORD=$(echo -n "$CASSANDRA_PASSWORD" | tr -d '\r\n[:space:]')

else
    echo "\n[Error] Unable to write secrets. Is there some secret missing?"
    exit 1
fi

until cqlsh cassandra -e "describe keyspaces"; do
  echo "Waiting for Cassandra..."
  sleep 5
done

cqlsh cassandra -e "CREATE KEYSPACE IF NOT EXISTS traveler_vpn WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '3'};"

exec "$@"