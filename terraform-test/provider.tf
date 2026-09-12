terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 4.6.0"
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

provider "docker" {}

provider "local" {}

provider "random" {}
