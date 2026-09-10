output "panel_data" {
  value = {
    name = serverspace_server.panel.name
    address = serverspace_server.panel.public_ip_addresses[0]
    ssh_port = random_integer.ssh_port.result
    panel_port = random_integer.panel_port.result
  }
}
