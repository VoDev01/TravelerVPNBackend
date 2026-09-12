output "instances" {
  value = merge(
    try({ for idx, instance in module.nodes : instance.node_data.name => instance.node_data }, {}),
    try({ for idx, instance in module.panel : instance.node_data.name => instance.node_data }, {}),
    try({ for idx, instance in module.spring_backend : instance.node_data.name => instance.node_data }, {})
  )
}

resource "local_file" "ansible_inventory" {
  content = yamlencode({
    all = {
      children = {
        "spring_backends" = {
          hosts = {
            for instance in module.spring_backend :
            instance.node_data.name => {
              ansible_host = instance.node_data.ansible_host
              ansible_connection = instance.node_data.ansible_connection
              access_port = 443
            }
          }
        }
        "nodes" = {
          hosts = {
            for instance in flatten(module.nodes[*].node_data) : instance.name => {
              ansible_host       = instance.ansible_host
              ansible_connection = instance.ansible_connection
              access_port        = instance.inbound_port
              location           = instance.location
            }
          }
        }
        "panels" = {
          hosts = {
            for instance in module.panel :
            instance.node_data.name => {
              ansible_host = instance.node_data.ansible_host
              ansible_connection = instance.node_data.ansible_connection
              access_port = instance.node_data.panel_port
            }
          }
        }
      }
    }
  })
  filename = "${path.module}/../ansible/inventories/staging/terraform_hosts.yml"
}
