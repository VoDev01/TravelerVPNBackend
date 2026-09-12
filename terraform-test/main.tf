resource "docker_network" "molecule_network" {
  name = "molecule"
  driver = "bridge"
}

module "nodes" {
    source = "./modules/nodes"
    network_id = docker_network.molecule_network.id
    node_count = 3
}

module "panel" {
    source = "./modules/panel"
    count = var.enable_panel ? 1 : 0
    network_id = docker_network.molecule_network.id
}

module "spring_backend" {
    source = "./modules/spring_backend"
    count = var.enable_backend ? 1 : 0
    network_id = docker_network.molecule_network.id
}
