output "node_data" {
  value = {
    name = serverspace_server.vless_node.name
    address = serverspace_server.vless_node.public_ip_addresses[0]
    ssh_port = random_integer.ssh_port.result
    inbound_port = random_integer.inbound_port.result
    location = var.location
  }
}
