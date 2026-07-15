vault {
  address = "http://vault:8200"
}

auto_auth {
  method "approle" {
    config = {
      role_id_file_path = "/vault/secrets/roleID"
      secret_id_file_path = "/vault/secrets/wrappedSecretID"
      secret_id_response_wrapping_path = "auth/approle/role/spring/secret-id"
      remove_secret_id_file_after_reading = true
    }
  }

  sink "file" {
    config = {
      path = "/vault/secrets/vault-token"
    }
  }
}