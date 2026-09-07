resource "docker_network" "molecule_network" {
  name = "molecule"
  driver = "bridge"
}

module "nodes" {
    source = "./modules/nodes"
    count = var.enable_node ? 1 : 0
    network_id = docker_network.molecule_network.id
}

module "panel" {
    source = "./modules/panel"
    count = var.enable_panel ? 1 : 0
    network_id = docker_network.molecule_network.id
}

module "spring-backend" {
    source = "./modules/spring-backend"
    count = var.enable_backend ? 1 : 0
    network_id = docker_network.molecule_network.id
}