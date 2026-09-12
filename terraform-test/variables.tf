variable "node_count" {
  type = number
  description = "3x-ui nodes count."
  default = 3
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
