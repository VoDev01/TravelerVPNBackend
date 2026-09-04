#!/bin/sh

set -e

echo "[Init] Using role-id for vault-agent"

SECRET_FILE="/run/secrets/app_secrets"

if [ -f "$SECRET_FILE" ]; then
  set -a
  source "$SECRET_FILE"
  set +a

  export VAULT_ROLE_ID=$(echo -n "$VAULT_ROLE_ID" | tr -d '\r\n[:space:]')

else
  echo "\n[Error] Unable to get role-id."
  exit 1
fi

echo "[Init] Role-id received successfully."
exit 0