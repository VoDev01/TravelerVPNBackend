terraform {
  required_providers {
    serverspace = {
      source = "itglobalcom/serverspace"
      version = "~> 0.3.2"
    }
    random = {
      source = "hashicorp/random"
      version = ">= 3.9.0"
    }
  }
}

resource "serverspace_ssh" "node_key" {
  name = "terraform-key"
  public_key = file("~/.ssh/vpn_vodev_ssh.pub")
}

resource "random_integer" "ssh_port" {
  min = 64000
  max = 65535
}

resource "random_integer" "inbound_port" {
  min = 1000
  max = 64399
}

resource "serverspace_server" "vless_node" {
  name = "node-${var.location}"
  image = "Ubuntu-22.04-X64"
  ram = 1024
  cpu = 1
  boot_volume_size = 25 * 1024
  location = var.location

  volume {
    name = "vol1"
    size = 25 * 1024
  }

  nic {
    network = ""
    network_type = "PublicShared"
    bandwidth = 50
  }

  ssh_keys = [
    resource.serverspace_ssh.node_key.id
  ]

  connection {
    host        = self.public_ip_addresses[0]
    user        = "root"
    type        = "ssh"
    private_key = file("~/.ssh/vpn_vodev_ssh.pem")
    timeout     = "1m"
  }
}
