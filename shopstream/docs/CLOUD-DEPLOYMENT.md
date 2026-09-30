# Deploying to the cloud

Local Docker Compose is enough for learning, and deploying to the cloud is optional. It costs real money (roughly USD 10-15 a day on AWS for EKS, a NAT gateway, RDS and MSK; somewhat less on Azure), so **tear everything down when you are done**.

The deployment is the same shape on both clouds:

| Piece | Local | AWS | Azure |
|---|---|---|---|
| Containers | Docker Compose | EKS (Kubernetes) | AKS (Kubernetes) |
| PostgreSQL | container | RDS | Azure Database for PostgreSQL |
| Kafka | container | MSK | Event Hubs (Kafka endpoint) |
| Images | built locally | GitHub Container Registry (ghcr.io) | Azure Container Registry |
| Public entry point | localhost:4200 | Network Load Balancer | Azure Load Balancer |

The Kubernetes manifests live in `infra/k8s`. `base/` holds everything shared; `overlays/aws` and `overlays/azure` change only what differs (addresses, secrets, image names, load balancer type).

Preview exactly what will be applied, without a cluster:

```bash
cp infra/k8s/overlays/aws/secrets.env.example infra/k8s/overlays/aws/secrets.env
kubectl kustomize infra/k8s/overlays/aws
```

---

## AWS: EKS + RDS + MSK

**Tools:** AWS CLI (logged in with `aws configure` or SSO), Terraform 1.6+, kubectl.

### 1. Create the infrastructure

```bash
cd infra/terraform/aws
export TF_VAR_db_password='choose-a-strong-password'
terraform init
terraform plan          # read it: this is what will be created
terraform apply         # about 20-30 minutes, mostly EKS and MSK
```

Note the outputs: `rds_endpoint`, `msk_bootstrap_brokers` and `configure_kubectl`.

### 2. Connect kubectl

```bash
aws eks update-kubeconfig --name shopstream --region us-east-1
kubectl get nodes
```

The autoscalers (`hpa.yaml`) need metrics-server:

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

### 3. Get images into a registry

Push the repo to GitHub. On every push to `main`, the CI workflow builds and pushes `ghcr.io/<your-user>/shopstream-<service>:<git-sha>` for each service.

GHCR packages are private by default. Either make each package public (GitHub → your profile → Packages → package → Settings → Change visibility), or create a pull secret and add `imagePullSecrets` to the Deployments.

### 4. Configure the overlay

- `infra/k8s/overlays/aws/kustomization.yaml`: replace `your-github-user` with your GitHub user name (lower-case).
- `infra/k8s/overlays/aws/config-patch.yaml`: paste `rds_endpoint` and `msk_bootstrap_brokers`.
- `cp secrets.env.example secrets.env` and fill it in. `DB_PASSWORD` is the Terraform password. git ignores this file.

### 5. Deploy

```bash
kubectl apply -k infra/k8s/overlays/aws
kubectl -n shopstream get pods -w                  # wait for everything to be Running and READY
kubectl -n shopstream get service frontend         # EXTERNAL-IP is the load balancer's DNS name
```

The `db-init` Job creates the six databases on RDS first. Services that start before it finishes restart once or twice; that is expected.

### 6. Deploy from GitHub Actions instead (optional)

`.github/workflows/deploy-aws.yml` deploys a chosen image tag with a button in the Actions tab. It uses GitHub's OIDC provider, so no AWS keys are stored in GitHub:

1. In IAM, add an identity provider for `token.actions.githubusercontent.com` with audience `sts.amazonaws.com`.
2. Create a role that trusts it, limited to your repository:
   ```json
   {
     "Effect": "Allow",
     "Principal": { "Federated": "arn:aws:iam::<account-id>:oidc-provider/token.actions.githubusercontent.com" },
     "Action": "sts:AssumeRoleWithWebIdentity",
     "Condition": {
       "StringEquals": { "token.actions.githubusercontent.com:aud": "sts.amazonaws.com" },
       "StringLike": { "token.actions.githubusercontent.com:sub": "repo:<your-user>/<your-repo>:*" }
     }
   }
   ```
3. Give the role `eks:DescribeCluster`, and map it into the cluster with an EKS access entry (`aws eks create-access-entry` + `associate-access-policy` with `AmazonEKSClusterAdminPolicy`, or a narrower policy).
4. In the GitHub repo settings, add the secret `AWS_DEPLOY_ROLE_ARN` and the variables `AWS_REGION` and `EKS_CLUSTER_NAME`. Create an environment named `production` with yourself as a required reviewer, which gives you a manual approval step.
5. The workflow cannot read your local `secrets.env`, so create the Secret in the cluster once by hand (step 5 above does that), or move secrets to AWS Secrets Manager with the External Secrets Operator.

### 7. Tear down

```bash
kubectl delete -k infra/k8s/overlays/aws    # removes the load balancer first
cd infra/terraform/aws && terraform destroy
```

---

## Azure: AKS + PostgreSQL + Event Hubs

**Tools:** Azure CLI (`az login`), kubectl, Docker.

### 1. Create the infrastructure

```bash
export PG_PASSWORD='choose-a-strong-password'
./infra/azure/provision.sh
```

The script prints the values you need next, including the Event Hubs connection string.

### 2. Build and push images to ACR

```bash
ACR=<your-acr-name>
az acr login --name $ACR
TAG=$(git rev-parse --short HEAD)
for s in api-gateway user-service product-service inventory-service order-service payment-service notification-service; do
  docker build -f backend.Dockerfile --build-arg SERVICE=$s -t $ACR.azurecr.io/shopstream-$s:$TAG .
  docker push $ACR.azurecr.io/shopstream-$s:$TAG
done
docker build -t $ACR.azurecr.io/shopstream-frontend:$TAG frontend
docker push $ACR.azurecr.io/shopstream-frontend:$TAG
```

On an Apple Silicon Mac, add `--platform linux/amd64` to `docker build`, because AKS nodes are x86 by default.

Then set the images in the overlay:

```bash
cd infra/k8s/overlays/azure
for s in api-gateway user-service product-service inventory-service order-service payment-service notification-service frontend; do
  kustomize edit set image shopstream/$s=$ACR.azurecr.io/shopstream-$s:$TAG
done
```

### 3. Configure and deploy

- `config-patch.yaml`: the PostgreSQL host and the Event Hubs namespace.
- `cp secrets.env.example secrets.env` and fill it in, including the `SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG` line.

```bash
kubectl apply -k infra/k8s/overlays/azure
kubectl -n shopstream get service frontend    # EXTERNAL-IP is the public address
```

How Event Hubs stands in for Kafka: the services do not change. Spring Boot turns the environment variables `SPRING_KAFKA_PROPERTIES_SECURITY_PROTOCOL=SASL_SSL`, `..._SASL_MECHANISM=PLAIN` and `..._SASL_JAAS_CONFIG` into Kafka client settings. Configuration, not code, decides which Kafka the app talks to.

### 4. Tear down

```bash
az group delete --name shopstream-rg --yes
```

---

## What this deployment leaves out

It is intentionally minimal. A production setup would add HTTPS (an Ingress or Gateway API controller with cert-manager, or a cloud load balancer with a certificate), a custom domain, secrets from Secrets Manager or Key Vault, private cluster endpoints, pod network policies, and centralized logging and monitoring.
