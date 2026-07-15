#!/bin/sh

set -e

export VAULT_TOKEN=$(cat /vault/secrets/vault-token)

exec "$@"