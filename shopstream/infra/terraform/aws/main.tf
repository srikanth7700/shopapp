# AWS infrastructure for ShopStream:
#   VPC (public + private subnets in 3 availability zones)
#   EKS  - managed Kubernetes that runs the containers
#   RDS  - managed PostgreSQL
#   MSK  - managed Kafka
#
# Cost warning: EKS, NAT gateway, RDS and MSK are billed per hour. Run
# `terraform destroy` when you are done experimenting.

data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  azs = slice(data.aws_availability_zones.available.names, 0, 3)
}

# ------------------------------------------------------------------ network

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = var.name
  cidr = "10.0.0.0/16"
  azs  = local.azs

  # Private subnets: EKS nodes, RDS and MSK. Not reachable from the internet.
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  # Public subnets: load balancers and the NAT gateway.
  public_subnets = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]

  enable_nat_gateway = true
  single_nat_gateway = true # one NAT gateway instead of three: cheaper, fine for a learning project

  # Tags that tell Kubernetes which subnets to use for public / internal load balancers.
  public_subnet_tags = {
    "kubernetes.io/role/elb" = 1
  }
  private_subnet_tags = {
    "kubernetes.io/role/internal-elb" = 1
  }
}

# ------------------------------------------------------------------ kubernetes

module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.0"

  cluster_name    = var.name
  cluster_version = var.kubernetes_version

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  # Lets you run kubectl from your laptop. Restrict with
  # cluster_endpoint_public_access_cidrs for anything beyond a demo.
  cluster_endpoint_public_access           = true
  enable_cluster_creator_admin_permissions = true

  cluster_addons = {
    coredns    = { most_recent = true }
    kube-proxy = { most_recent = true }
    vpc-cni    = { most_recent = true }
  }

  eks_managed_node_groups = {
    default = {
      instance_types = [var.node_instance_type]
      min_size       = 2
      max_size       = 4
      desired_size   = 2
    }
  }
}

# ------------------------------------------------------------------ postgres

resource "aws_db_subnet_group" "postgres" {
  name       = "${var.name}-postgres"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_security_group" "postgres" {
  name        = "${var.name}-postgres"
  description = "PostgreSQL reachable only from the EKS nodes"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "PostgreSQL from EKS nodes"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }
}

resource "aws_db_instance" "postgres" {
  identifier     = "${var.name}-postgres"
  engine         = "postgres"
  engine_version = "16"
  instance_class = "db.t4g.micro"

  allocated_storage = 20
  storage_encrypted = true

  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.postgres.name
  vpc_security_group_ids = [aws_security_group.postgres.id]
  publicly_accessible    = false

  backup_retention_period = 1
  skip_final_snapshot     = true # a learning database; set to false for real data
}

# ------------------------------------------------------------------ kafka

resource "aws_security_group" "msk" {
  name        = "${var.name}-msk"
  description = "Kafka reachable only from the EKS nodes"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "Kafka plaintext from EKS nodes"
    from_port       = 9092
    to_port         = 9092
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }

  ingress {
    description     = "Kafka TLS from EKS nodes"
    from_port       = 9094
    to_port         = 9094
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }
}

resource "aws_msk_configuration" "kafka" {
  name           = "${var.name}-kafka"
  kafka_versions = ["3.6.0"]

  server_properties = <<-PROPERTIES
    auto.create.topics.enable = true
    default.replication.factor = 3
    min.insync.replicas = 2
    num.partitions = 3
  PROPERTIES
}

resource "aws_msk_cluster" "kafka" {
  cluster_name           = var.name
  kafka_version          = "3.6.0"
  number_of_broker_nodes = 3 # one per availability zone

  broker_node_group_info {
    instance_type   = "kafka.t3.small"
    client_subnets  = module.vpc.private_subnets
    security_groups = [aws_security_group.msk.id]

    storage_info {
      ebs_storage_info {
        volume_size = 20
      }
    }
  }

  configuration_info {
    arn      = aws_msk_configuration.kafka.arn
    revision = aws_msk_configuration.kafka.latest_revision
  }

  encryption_info {
    encryption_in_transit {
      # Plaintext is allowed inside the VPC so the services need no TLS setup.
      # For production, use "TLS" and set SPRING_KAFKA_PROPERTIES_SECURITY_PROTOCOL=SSL.
      client_broker = "TLS_PLAINTEXT"
      in_cluster    = true
    }
  }
}
