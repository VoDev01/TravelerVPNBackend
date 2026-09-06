#!/bin/sh

set -euo pipefail

echo "\n[Info] Waiting for Vault API..."

while ! nc -z 127.0.0.1 8200; do
    printf '.'
    sleep 0.5
done

sleep 2

echo "[Info] Vault API is listeting. Initializing vault..."

SECRET_FILE="/vault/secrets/app_secrets.txt"

KEY_SHARES=1
KEY_THRESHOLD=1

DEV_MODE=false

if [ -f "$SECRET_FILE" ]; then
    set -a
    source "$SECRET_FILE"
    set +a

    if ! $DEV_MODE; then
        export VAULT_ROLE_ID=$(echo -n "$VAULT_ROLE_ID" | tr -d '\r\n[:space:]')

        INIT_OUT=$(vault operator init -key-shares=$KEY_SHARES -key-threshold=$KEY_THRESHOLD -format=json 2>/dev/null)

        if [ $? -ne 0 ] || [ -z "$INIT_OUT" ]; then
            echo "[Error] Vault is already initialized or failed to init."
            exit 1
        fi

        export VAULT_TOKEN=$(echo "$INIT_OUT" | jq -r '.root_token')
    else
        export VAULT_TOKEN="root"
    fi

    echo "[Info] Unsealing Vault..."

    if ! $DEV_MODE; then
        i=0

        THRESHOLD=$(echo "$INIT_OUT" | jq -r '.unseal_threshold')

        while [ "$i" -lt "$THRESHOLD" ]; do
            echo "[Info] Input key $((i+1)) out of $THRESHOLD..."

            CURRENT_KEY=$(echo "$INIT_OUT" | jq -r ".unseal_keys_b64[$i]")
            
            vault operator unseal "$CURRENT_KEY" > /dev/null
            
            if [ $? -ne 0 ]; then
                echo "[Error] inputting key $((i+1))!"
                exit 1
            fi
            i=$((i + 1))
        done
    fi

    echo "[Info] Vault unsealed!"

    vault secrets enable -path=secret kv-v2 2>/dev/null || true

    echo "[Info] Writing secrets..."

    vault kv put secret/spring \
        cassandraUser=$CASSANDRA_USER \
        cassandraPassword=$CASSANDRA_PASSWORD \
        marzbanUsername=$MARZBAN_USERNAME \
        marzbanPassword=$MARZBAN_PASSWORD \
        xuiToken=$XUI_API_TOKEN \
        xuiAccessUrl=$XUI_ACCESS_URL \
        xuiSubUrl=$XUI_SUB_URL \
        xuiUsername=$XUI_USERNAME \
        xuiPassword=$XUI_PASSWORD
else
    echo "[Error] Unable to write secrets. Is there some secret missing?"
    exit 1
fi

echo "[Info] Secrets written. Enabling authentication and adding roles..."

vault auth enable approle

vault policy write spring-app-policy /run/secrets/spring-app-policy

vault write auth/approle/role/spring \
    token_type=default \
    secret_id_ttl=60m \
    token_period=60m \
    secret_id_num_uses=60 \
    policies="spring-app-policy"

vault write auth/approle/role/spring/role-id role_id="$VAULT_ROLE_ID"

WRAPPED_TOKEN=$(vault write -field=wrapping_token -wrap-ttl=5m -f auth/approle/role/spring/secret-id)
CLEAN_TOKEN=$(echo "$WRAPPED_TOKEN" | tr -d '\r\n[:space:]')

echo -n "$VAULT_ROLE_ID" > /vault/secrets/roleID
echo -n "$CLEAN_TOKEN" > /vault/secrets/wrappedSecretID

echo "[Info] Vault is successfully provisioned."
exit 0