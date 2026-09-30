output "cluster_name" {
  description = "Use with: aws eks update-kubeconfig --name <cluster_name> --region <region>"
  value       = module.eks.cluster_name
}

output "rds_endpoint" {
  description = "Goes into DB_HOST in infra/k8s/overlays/aws/config-patch.yaml"
  value       = aws_db_instance.postgres.address
}

output "msk_bootstrap_brokers" {
  description = "Goes into KAFKA_BOOTSTRAP_SERVERS in infra/k8s/overlays/aws/config-patch.yaml"
  value       = aws_msk_cluster.kafka.bootstrap_brokers
}

output "configure_kubectl" {
  description = "Run this to point kubectl at the new cluster"
  value       = "aws eks update-kubeconfig --name ${module.eks.cluster_name} --region ${var.region}"
}
