terraform {
  required_providers {
    aeza = {
      source  = "scinfra-pro/aeza"
      version = "~> 0.3.0"
    }
  }

  required_version = "~> 1.15"
}

variable "aeza_api_key" {
  type      = string
  sensitive = true
  validation {
    condition     = length(var.aeza_api_key) > 0
    error_message = "API key must not be empty"
  }
}

variable "aeza_base_url" {
  description = "Base URL for Aeza API"
  type        = string
  default     = "https://my.aeza.net/api"
}

provider "aeza" {
  api_key = var.aeza_api_key
  base_url = var.aeza_base_url
}

data "aeza_service_types" "all" {}

data "aeza_products" "all" {}

resource "aeza_service" "test_node_server" {
  product_id   = 181
  os           = "ubuntu_2404"
  payment_term = "hour"
  name         = "test_node_server"
  auto_prolong = false
}

resource "aeza_service" "test_backend_server" {
  product_id   = 181
  os           = "ubuntu_2404"
  payment_term = "hour"
  name         = "test_backend_server"
  auto_prolong = false
}