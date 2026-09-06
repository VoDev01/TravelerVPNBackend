#!/bin/sh
sleep 5

export VAULT_TOKEN=$(cat /vault/secrets/vault-token) || exit 1

exec "$@"