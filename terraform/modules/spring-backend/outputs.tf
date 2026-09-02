output "vless_node" {
  description = "Vless node data"
  value = serverspace_server.vless_node
}

output "ssh_port" {
  description = "Vless node ssh port"
  value = var.node_ssh_port
}