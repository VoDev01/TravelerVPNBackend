output "instances" {
  value = merge(
    { for idx, instance in module.nodes : instance.node_data.name => instance.node_data },
    { for idx, instance in module.panel : instance.node_data.name => instance.node_data },
    { for idx, instance in module.spring_backend : instance.node_data.name => instance.node_data }
  )
}

resource "local_file" "ansible_inventory" {
  content = yamlencode({
    all = {
      children = {
        "spring_backends" = {
          hosts = {
            for idx, instance in module.spring_backend : 
            instance.node_data.name => {
              ansible_host = instance.node_data.ansible_host
              ansible_connection = instance.node_data.ansible_connection
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
            }
          }
        }
        "panels" = {
          hosts = {
            for idx, instance in module.panel : 
            instance.node_data.name => {
              ansible_host = instance.node_data.ansible_host
              ansible_connection = instance.node_data.ansible_connection
            }
          }
        }
      }
    }
  })
  filename = "${path.module}/../ansible/inventories/terraform_hosts.yml"
}