path "secret/data/spring" {
  capabilities = ["read"]
}

path "secret/data/spring/*" {
  capabilities = ["read"]
}

path "secret/data/cassandra" {
  capabilities = ["read"]
}

path "secret/data/cassandra/*" {
  capabilities = ["read"]
}