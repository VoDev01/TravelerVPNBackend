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

variable "node_ssh_port" {
  type = number
  validation {
    condition = var.node_ssh_port != null
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
      "sudo ufw allow ${var.node_ssh_port},${var.node_port},80",
      "sudo ufw allow out 80,443",
      "sudo ufw enable"
    ]
  }  
}

output "vless_node" {
  description = "Vless node data"
  value = serverspace_server.vless_node
}

output "panel" {
  description = "3x-ui panel data"
  value = serverspace_server.vless_node
}

output "spring_backend" {
  description = "Spring backend data"
  value = serverspace_server.vless_node
}

output "ssh_port" {
  description = "Vless node ssh port"
  value = var.node_ssh_port
}