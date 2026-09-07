variable "enable_node" {
  type = bool
  description = "Create 3x-ui node."
  default = true
}

variable "enable_backend" {
  type = bool
  description = "Create backend."
  default = true
}

variable "enable_panel" {
  type = bool
  description = "Create 3x-ui panel."
  default = true
}