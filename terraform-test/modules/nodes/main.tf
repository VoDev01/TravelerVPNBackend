terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 4.6.0"
    }
    local = {
      source  = "hashicorp/local"
      version = ">= 2.0.0"
    }
  }
}

resource "docker_image" "ubuntu_ansible" {
  name = "geerlingguy/docker-ubuntu2204-ansible:latest"
  force_remove = true
}

resource "docker_container" "vps_test_container" {
  name  = "3x-node"
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
    internal = 443
  }

  networks_advanced {
    name = var.network_id
  }
}