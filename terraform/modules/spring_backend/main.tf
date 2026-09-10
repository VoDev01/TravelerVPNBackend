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

resource "random_integer" "ssh_port" {
  min = 1000
  max = 65535
}

resource "serverspace_server" "spring_backend" {
  name = "spring-backend-${var.location}"
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
    private_key = file("./vpn_vodev_ssh.pem")
    timeout     = "1m"
  }

  provisioner "remote-exec" {
    inline = [
      "export PATH=$PATH:/usr/bin",
      "sudo ufw allow ${random_integer.ssh_port.result},443,80",
      "sudo ufw allow out 80,443",
      "sudo ufw enable"
    ]
  }
}
