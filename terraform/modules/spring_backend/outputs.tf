output "spring_backend_data" {
  value = {
    name = serverspace_server.panel.name
    ansible_host = serverspace_server.panel.public_ip_addresses[0]
    ansible_port = random_integer.ssh_port.result
  }
}
