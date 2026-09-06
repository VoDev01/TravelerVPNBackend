terraform {
  required_providers {
    vault = {
      source  = "hashicorp/vault"
      version = "~> 4.0"
    }
  }
}

variable "vault_addr" {
  description = "The address of the Vault server"
  type        = string
  default     = "http://127.0.0.1:8200"
}

variable "vault_token" {
  description = "Vault Root Token (only if not using Kubernetes Auth)"
  type        = string
  sensitive   = true
}

locals {
  vault_secrets = jsondecode(file("secrets.json"))
}

variable "cassandra_user" {
  type = string
}
variable "cassandra_password" {
  type = string
}
variable "xui_username" {
  type = string
}
variable "xui_password" {
  type = string
}
variable "serverspace_token" {
  type = string
  sensitive   = true
}

resource "vault_approle_auth_method_role" "spring" {
  mount_path          = "auth/approle"
  path                = "role/spring"
  
  token_ttl           = "60m"
  token_period        = "60m"
  bind_secret_id_ttl  = "60m"

  policy              = vault_policy.spring.policy_name
}

resource "vault_policy" "spring" {
  name    = "spring-app-policy"
  policy  = file("policies/spring-app-policy.hcl")
}

resource "vault_kv_secret_v2" "app_secrets" {
  name = "secret/spring"
  mount    = "secret/spring"
  
  data_json = jsonencode({
    cassandraUser           = var.cassandra_user
    cassandraPassword       = var.cassandra_password
    xuiUsername              = var.xui_username
    xuiPassword              = var.xui_password
    serverspace_token        = var.serverspace_token
  })

  depends_on           = [vault_policy.spring]
}

resource "local_file" "role_id" {
  content         = vault_approle_auth_method_role.spring.role_id
  file_permission = "0640"
  filename            = "/vault/secrets/roleID"
}

resource "local_file" "wrapped_secret_id" {
  content    = vault_kv_secret_v2.app_secrets.id
  file_permission = "0640"
  filename            = "/vault/secrets/wrappedSecretID"
}
