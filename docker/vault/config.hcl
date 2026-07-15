ui            = true
api_addr      = "http://127.0.0.1:8200"
disable_mlock = true
cluster_addr = "http://127.0.0.1:8201"

listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_disable = true
  //tls_cert_file = "/vault/config/certs/vault.crt"
  //tls_key_file  = "/vault/config/certs/vault.key"
}

storage "file" {
  path = "/vault/data"
}