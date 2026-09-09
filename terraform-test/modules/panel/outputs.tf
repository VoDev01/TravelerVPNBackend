output "node_data" {
  value = {
    name         = docker_container.vps_test_container.name
    ansible_host = docker_container.vps_test_container.name
    ansible_connection = "docker"
  }
}