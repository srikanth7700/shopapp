variable "region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "us-east-1"
}

variable "name" {
  description = "Name prefix for every resource"
  type        = string
  default     = "shopstream"
}

variable "kubernetes_version" {
  description = "EKS Kubernetes version"
  type        = string
  default     = "1.33" # pick a version that is in standard support when you deploy
}

variable "node_instance_type" {
  description = "EC2 instance type for the EKS worker nodes. 7 JVM services need roughly 4-6 GB of memory in total."
  type        = string
  default     = "t3.large"
}

variable "db_username" {
  description = "Master user for the RDS PostgreSQL instance"
  type        = string
  default     = "shopstream"
}

variable "db_password" {
  description = "Master password for RDS. Pass it with TF_VAR_db_password, never commit it."
  type        = string
  sensitive   = true
}
