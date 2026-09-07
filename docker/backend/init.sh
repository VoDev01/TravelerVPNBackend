#!/bin/sh

if [ -f /vault/secrets/vault-token ]; then
    export VAULT_TOKEN=$(cat /vault/secrets/vault-token)
else
    echo "Warning: /vault/secrets/vault-token not found!"
fi

exec "$@"