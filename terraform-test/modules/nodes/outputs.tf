output "node_data" {
  value = [
    for node in docker_container.vps_test_container : {
      name               = node.name
      ansible_host       = node.name
      inbound_port       = node.ports[0].external
      ansible_connection = "docker"
      location           = "am2"
    }
  ]
}
