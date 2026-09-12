terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 4.6.0"
    }
    random = {
      source = "hashicorp/random"
      version = ">= 3.9.0"
    }
  }
}

resource "random_integer" "inbound_port" {
  count = var.node_count

  min = 1000
  max = 64399

  keepers = {
    id = var.node_count
  }
}

resource "docker_image" "ubuntu_ansible" {
  name = "geerlingguy/docker-ubuntu2204-ansible:latest"
  force_remove = true
}

resource "docker_container" "vps_test_container" {
  count = var.node_count

  name  = "3x-node-${count.index}"
  image = docker_image.ubuntu_ansible.image_id

  command = ["/lib/systemd/systemd"]

  privileged = true
  cgroupns_mode = "host"

  volumes {
    host_path      = "/sys/fs/cgroup"
    container_path = "/sys/fs/cgroup"
    read_only      = false
  }

  volumes {
    host_path      = "/var/run/docker.sock"
    container_path = "/var/run/docker.sock"
  }

  ports {
    internal = random_integer.inbound_port[count.index].result
    external = random_integer.inbound_port[count.index].result
  }

  networks_advanced {
    name = var.network_id
  }
}
