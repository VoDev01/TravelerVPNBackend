terraform {
  required_providers {
    serverspace = {
      source = "itglobalcom/serverspace"
      version = "~> 0.3.2"
    }
  }
}

variable "serverspace_token" {
  type = string
  sensitive = true
}

provider "serverspace" {
  key = var.serverspace_token
  host = "https://api.serverspace.ru"
}

