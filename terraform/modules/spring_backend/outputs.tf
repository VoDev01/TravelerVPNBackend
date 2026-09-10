output "spring_backend_data" {
  value = {
    name = serverspace_server.spring_backend.name
    address = serverspace_server.spring_backend.public_ip_addresses[0]
    ssh_port = random_integer.ssh_port.result
  }
}
