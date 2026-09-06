#!/bin/sh

set -euo pipefail

export VAULT_TOKEN=$(cat /vault/secrets/vault-token)

exec "$@"