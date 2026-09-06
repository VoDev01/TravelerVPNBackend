resource "local_file" "ansible_inventory" {
  content = yamlencode({
    all = {
      "3x-nodes" = {
        hosts = {
          name = serverspace_server.vless_node.name
          ip = serverspace_server.vless_node.public_ip_addresses[0]
          ssh_port = var.node_ssh_port
        }
      }
    }
  })
  filename = "${path.module}/../ansible/inventory/terraform_hosts.yml.yml"
}