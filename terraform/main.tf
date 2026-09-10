module "nodes" {
  source = "./modules/nodes"
  count = var.node_count
  location = var.node_location
}

module "panel" {
  source = "./modules/panel"
  count = var.enable_panel ? 1 : 0
  location = var.panel_location
}

module "spring_backend" {
  source = "./modules/spring_backend"
  count = var.enable_backend ? 1 : 0
  location = var.panel_location
}
