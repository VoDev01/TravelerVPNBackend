# syntax=docker/dockerfile:1
FROM hashicorp/vault:latest

WORKDIR /vault

COPY ./docker/vault/config.hcl config.hcl

CMD ["vault", "server", "-config", "config.hcl"]