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