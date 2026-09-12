output "node_data" {
  value = {
    name         = docker_container.vps_test_container.name
    ansible_host = docker_container.vps_test_container.name
    panel_port = docker_container.vps_test_container.ports[0].external
    ansible_connection = "docker"
  }
}
