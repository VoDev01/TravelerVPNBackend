output "instances" {
  value = merge(
    { for idx, instance in module.nodes : "${instance.node_data.name}-${idx + 1}" => instance.node_data },
    { for idx, instance in module.panel : "${instance.panel_data.name}-${idx + 1}" => instance.panel_data },
    { for idx, instance in module.spring_backend : "${instance.spring_backend_data.name}-${idx + 1}" => instance.spring_backend_data }
  )
}

resource "local_file" "ansible_inventory" {
  content = yamlencode({
    all = {
      children = {
        "spring_backends" = {
          hosts = {
            for idx, instance in module.spring_backend :
            "${instance.spring_backend_data.name}-${idx + 1}" => {
              ansible_host = instance.spring_backend_data.address
              ssh_port = instance.spring_backend_data.ssh_port
              access_port = 443
              ansible_ssh_private_key_file = "~/.ssh/provisioner.pem"
            }
          }
        }
        "nodes" = {
          hosts = {
            for idx, instance in module.nodes :
            "${instance.node_data.name}-${idx + 1}" => {
              ansible_host = instance.node_data.address
              ssh_port = instance.node_data.ssh_port
              access_port = instance.node_data.inbound_port
              location = instance.node_data.location
              ansible_ssh_private_key_file = "~/.ssh/provisioner.pem"
            }
          }
        }
        "panels" = {
          hosts = {
            for idx, instance in module.panel :
            "${instance.panel_data.name}-${idx + 1}" => {
              ansible_host = instance.panel_data.address
              ssh_port = instance.panel_data.ssh_port
              access_port = instance.panel_data.panel_port
              ansible_ssh_private_key_file = "~/.ssh/provisioner.pem"
            }
          }
        }
      }
    }
  })
  filename = "${path.module}/../ansible/inventories/production/terraform_hosts.yml"
}
