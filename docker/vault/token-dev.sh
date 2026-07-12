#!/bin/sh

echo "Ожидание готовности Vault API..."

while ! nc -z 0.0.0.0 8200; do
    printf '.'
    sleep 0.5
done

sleep 1.5

rm -f /token-share/vault.env

echo -e "\n[ОК] Сетевой порт Vault открыт! Генерация токенов..."

FINAL_TOKEN=$(vault token create -field=token)
WRAPPED_TOKEN=$(vault token create -wrap-ttl=300s -field=wrapping_token)

if [ -z "$WRAPPED_TOKEN" ]; then
    echo "[ОШИБКА] Не удалось получить WRAPPED_TOKEN!"
    exit 1
fi

echo "SPRING_CLOUD_VAULT_TOKEN=$WRAPPED_TOKEN" > /shared-data/vault.env
echo "[УСПЕХ] Токен успешно записан в /shared-data/vault.env"

exit 0