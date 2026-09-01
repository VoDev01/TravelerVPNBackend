variable "location" {
  type = string
  default = "am2"
}

variable "node_port" {
  type = number
  validation {
    condition = var.node_port != null
    error_message = "Port cant be empty."
  }
}

variable "ssh_port" {
  type = number
  validation {
    condition = var.ssh_port != null
    error_message = "SSH port cant be empty."
  }
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
    private_key = file("./vless_node.pem")
    timeout     = "1m"
  }

  provisioner "remote-exec" {
    inline = [
      "export PATH=$PATH:/usr/bin",
      "sudo ufw allow ssh,${var.node_port},80",
      "sudo ufw allow out 80,443",
      "sudo ufw enable"
    ]
  }  
}

output "vless_node" {
  description = "Vless node data"
  value = serverspace_server.vless_node
}