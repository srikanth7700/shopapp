terraform {
  required_version = ">= 1.6"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # For a team, keep state in S3 so everyone shares it:
  # backend "s3" {
  #   bucket = "my-terraform-state"
  #   key    = "shopstream/terraform.tfstate"
  #   region = "us-east-1"
  # }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project   = "shopstream"
      ManagedBy = "terraform"
    }
  }
}
