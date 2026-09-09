resource "local_file" "ansible_inventory" {
  content = yamlencode({
    all = {
      children = {
        "3x-nodes" = {
          hosts = {
            (docker_container.vps_test_container.name) = {
              ansible_host = "127.0.0.1"
              ansible_port = docker_container.vps_test_container.ports[0].external
            }
          }
        }
      }
    }
  })
  filename = "${path.module}/../../../ansible/inventory/terraform_hosts.yml"
}

output "container_info" {
  value = {
    name = docker_container.vps_test_container.name
  }
}
