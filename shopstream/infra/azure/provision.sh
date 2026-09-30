#!/usr/bin/env bash
#
# Creates the Azure infrastructure for ShopStream with the Azure CLI:
#   Resource group, Azure Container Registry (ACR), AKS (Kubernetes),
#   Azure Database for PostgreSQL flexible server, Event Hubs (Kafka endpoint).
#
# Usage:
#   az login
#   export PG_PASSWORD='a-strong-password'
#   ./infra/azure/provision.sh
#
# Cost warning: AKS nodes, PostgreSQL and Event Hubs Standard are billed per
# hour. Delete everything afterwards with:
#   az group delete --name "$RESOURCE_GROUP" --yes
set -euo pipefail

RESOURCE_GROUP="${RESOURCE_GROUP:-shopstream-rg}"
LOCATION="${LOCATION:-eastus}"
# ACR, PostgreSQL and Event Hubs names must be globally unique: a suffix is added.
SUFFIX="${SUFFIX:-$RANDOM}"
ACR_NAME="${ACR_NAME:-shopstreamacr${SUFFIX}}"
AKS_NAME="${AKS_NAME:-shopstream-aks}"
PG_SERVER="${PG_SERVER:-shopstream-pg-${SUFFIX}}"
PG_USER="${PG_USER:-shopstream}"
EVENTHUBS_NAMESPACE="${EVENTHUBS_NAMESPACE:-shopstream-events-${SUFFIX}}"
: "${PG_PASSWORD:?Set PG_PASSWORD first, e.g. export PG_PASSWORD='...'}"

echo "==> Resource group ${RESOURCE_GROUP} in ${LOCATION}"
az group create --name "$RESOURCE_GROUP" --location "$LOCATION" --output none

echo "==> Container registry ${ACR_NAME}"
az acr create --resource-group "$RESOURCE_GROUP" --name "$ACR_NAME" --sku Basic --output none

echo "==> AKS cluster ${AKS_NAME} (takes several minutes)"
az aks create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$AKS_NAME" \
  --node-count 2 \
  --node-vm-size Standard_B4ms \
  --attach-acr "$ACR_NAME" \
  --generate-ssh-keys \
  --output none
az aks get-credentials --resource-group "$RESOURCE_GROUP" --name "$AKS_NAME" --overwrite-existing

echo "==> PostgreSQL flexible server ${PG_SERVER}"
az postgres flexible-server create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$PG_SERVER" \
  --location "$LOCATION" \
  --admin-user "$PG_USER" \
  --admin-password "$PG_PASSWORD" \
  --tier Burstable \
  --sku-name Standard_B1ms \
  --storage-size 32 \
  --version 16 \
  --public-access 0.0.0.0 \
  --yes \
  --output none
# --public-access 0.0.0.0 allows connections from Azure services (including AKS).
# For production, use private networking (VNet integration) instead.

echo "==> Event Hubs namespace ${EVENTHUBS_NAMESPACE} (Standard tier includes the Kafka endpoint)"
az eventhubs namespace create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$EVENTHUBS_NAMESPACE" \
  --location "$LOCATION" \
  --sku Standard \
  --output none

# In Event Hubs, a Kafka topic is an "event hub". Create the topics and their
# dead-letter topics up front.
for topic in order-events inventory-events payment-events product-events; do
  for name in "$topic" "${topic}.DLT"; do
    az eventhubs eventhub create \
      --resource-group "$RESOURCE_GROUP" \
      --namespace-name "$EVENTHUBS_NAMESPACE" \
      --name "$name" \
      --partition-count 3 \
      --output none
  done
done

CONNECTION_STRING=$(az eventhubs namespace authorization-rule keys list \
  --resource-group "$RESOURCE_GROUP" \
  --namespace-name "$EVENTHUBS_NAMESPACE" \
  --name RootManageSharedAccessKey \
  --query primaryConnectionString --output tsv)

cat <<SUMMARY

Done. Next steps (details in docs/CLOUD-DEPLOYMENT.md):

1. Push images to ACR:
     az acr login --name ${ACR_NAME}
     # then build and push each service as ${ACR_NAME}.azurecr.io/shopstream-<service>:<tag>

2. In infra/k8s/overlays/azure/config-patch.yaml set:
     DB_HOST:                 ${PG_SERVER}.postgres.database.azure.com
     KAFKA_BOOTSTRAP_SERVERS: ${EVENTHUBS_NAMESPACE}.servicebus.windows.net:9093
   and in kustomization.yaml replace yourregistry.azurecr.io with ${ACR_NAME}.azurecr.io

3. Create infra/k8s/overlays/azure/secrets.env from secrets.env.example with:
     DB_USERNAME=${PG_USER}
     DB_PASSWORD=<your PG_PASSWORD>
     SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG=org.apache.kafka.common.security.plain.PlainLoginModule required username="\$ConnectionString" password="${CONNECTION_STRING}";

4. Deploy:
     kubectl apply -k infra/k8s/overlays/azure
     kubectl -n shopstream get service frontend   # EXTERNAL-IP is the public address
SUMMARY
