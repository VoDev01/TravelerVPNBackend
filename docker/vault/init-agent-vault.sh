#!/bin/sh

set -e

echo "[Init] Vault is ready. Generating AppRole credentials..."

SECRET_FILE="/run/secrets/vault_secrets"

if [ -f "$SECRET_FILE" ]; then
  set -a
  source "$SECRET_FILE"
  set +a

  export VAULT_ROLE_ID=$(echo -n "$VAULT_ROLE_ID" | tr -d '\r\n[:space:]')
  export VAULT_TOKEN=$(echo -n "$VAULT_TOKEN" | tr -d '\r\n[:space:]')
else
    echo "\n[Error] Unable to get secrets. Is there some secret missing?"
    exit 1
fi

echo -n "$VAULT_ROLE_ID" > /vault/secrets/roleID

WRAPPED_TOKEN=$(vault write -field=wrapping_token -wrap-ttl=5m -f auth/approle/role/spring/secret-id)

CLEAN_TOKEN=$(echo "$WRAPPED_TOKEN" | tr -d '\r\n[:space:]')

echo -n "$CLEAN_TOKEN" > /vault/secrets/wrappedSecretID

echo "[Init] Complete! Shared credentials provisioned successfully."

exit 0