#!/bin/sh

set -e

echo "[Init] Vault is ready. Generating AppRole credentials..."

echo -n "$VAULT_ROLE_ID" > /vault/secrets/roleID

WRAPPED_TOKEN=$(vault write -field=wrapping_token -wrap-ttl=5m -f auth/approle/role/spring/secret-id)

CLEAN_TOKEN=$(echo "$WRAPPED_TOKEN" | tr -d '\r\n[:space:]')

echo -n "$CLEAN_TOKEN" > /vault/secrets/wrappedSecretID

echo "[Init] Complete! Shared credentials provisioned successfully."

exit 0