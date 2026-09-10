terraform {
  required_providers {
    serverspace = {
      source = "itglobalcom/serverspace"
      version = "~> 0.3.2"
    }
    local = {
      source  = "hashicorp/local"
      version = ">= 2.0.0"
    }
    random = {
      source = "hashicorp/random"
      version = ">= 3.9.0"
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

provider "local" {}

provider "random" {}
