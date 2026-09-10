variable "node_location" {
  type = string
  default = "am2"
}

variable "spring_backend_location" {
  type = string
  default = "am2"
}

variable "panel_location" {
  type = string
  default = "am2"
}

variable "node_count" {
  type = number
  description = "Node count."
  default = 1
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
