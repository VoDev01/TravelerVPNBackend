output "instances" {
  value = merge(
    { for idx, instance in module.nodes : instance.node_data.name => instance.node_data },
    { for idx, instance in module.panel : instance.panel_data.name => instance.panel_data },
    { for idx, instance in module.spring_backend : instance.spring_backend_data.name => instance.spring_backend_data }
  )
}

resource "local_file" "ansible_inventory" {
  content = yamlencode({
    all = {
      children = {
        "spring_backends" = {
          hosts = {
            for idx, instance in module.spring_backend :
            instance.spring_backend_data.name => {
              ansible_host = instance.spring_backend_data.ansible_host
              ansible_connection = instance.spring_backend_data.ansible_connection
            }
          }
        }
        "nodes" = {
          hosts = {
            for idx, instance in module.nodes :
            instance.node_data.name => {
              ansible_host = instance.node_data.ansible_host
              ansible_connection = instance.node_data.ansible_connection
              inbound_port = instance.node_data.inbound_port
              location = instance.node_data.location
            }
          }
        }
        "panels" = {
          hosts = {
            for idx, instance in module.panel :
            instance.panel_data.name => {
              ansible_host = instance.panel_data.ansible_host
              ansible_connection = instance.panel_data.ansible_connection
              panel_port = instance.panel_data.panel_port
            }
          }
        }
      }
    }
  })
  filename = "${path.module}/../ansible/inventories/terraform_hosts.yml"
}
