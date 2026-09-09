output "node_data" {
  value = {
    name         = docker_container.vps_test_container.name
    ansible_host = docker_container.vps_test_container.name
    inbound_port = 8433
    ansible_connection = "docker"
  }
}
