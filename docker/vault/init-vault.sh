#!/bin/sh

set -e

echo -e "\n[Info] Waiting for Vault API..."

while ! nc -z 127.0.0.1 8200; do
    printf '.'
    sleep 0.5
done

sleep 2

echo -e "\n[Info] Vault is ready! Writing secrets..."

vault kv put secret/spring \
  cassandraUser=$CASSANDRA_USER \
  cassandraPassword=$CASSANDRA_PASSWORD \
  marzbanUsername=$MARZBAN_USERNAME \
  marzbanPassword=$MARZBAN_PASSWORD

echo -e "\n[Info] Secrets written. Enabling authentication and adding roles..."

vault auth enable approle

vault policy write spring-app-policy /run/secrets/spring-app-policy

vault write auth/approle/role/spring \
    token_type=default \
    secret_id_ttl=60m \
    token_ttl=60m \
    token_max_ttl=24h \
    token_period=60m \
    secret_id_num_uses=60 \
    policies="spring-app-policy"

vault write auth/approle/role/spring/role-id role_id="$VAULT_SPRING_ROLE_ID"

echo -e "\n[Info] Authentication is ready! Allowed to fetch secrets."
exit 0