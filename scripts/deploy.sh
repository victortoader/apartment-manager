#!/usr/bin/env bash

set -Eeuo pipefail

APP_DIR="/home/ubuntu/apartment-manager"
BRANCH="main"
SSM_PREFIX="/apartment-manager"

cd "$APP_DIR"

echo "Downloading latest code..."
git fetch origin "$BRANCH"

echo "Updating local working copy..."
git reset --hard "origin/$BRANCH"

echo "Fetching secrets from Parameter Store..."
export DB_USERNAME=$(aws ssm get-parameter --name "$SSM_PREFIX/DB_USERNAME" --with-decryption --query "Parameter.Value" --output text)
export DB_PASSWORD=$(aws ssm get-parameter --name "$SSM_PREFIX/DB_PASSWORD" --with-decryption --query "Parameter.Value" --output text)
export JWT_SECRET=$(aws ssm get-parameter --name "$SSM_PREFIX/JWT_SECRET" --with-decryption --query "Parameter.Value" --output text)

echo "Rebuilding and restarting application..."
docker compose up -d --build --remove-orphans

echo "Removing unused Docker images..."
docker image prune -f

echo "Deployment completed."
docker compose ps