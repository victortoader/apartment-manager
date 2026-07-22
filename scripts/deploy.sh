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
DB_USERNAME=$(aws ssm get-parameter --name "$SSM_PREFIX/DB_USERNAME" --with-decryption --query "Parameter.Value" --output text)
DB_PASSWORD=$(aws ssm get-parameter --name "$SSM_PREFIX/DB_PASSWORD" --with-decryption --query "Parameter.Value" --output text)
JWT_SECRET=$(aws ssm get-parameter --name "$SSM_PREFIX/JWT_SECRET" --with-decryption --query "Parameter.Value" --output text)
DEFAULT_PASSWORD=$(aws ssm get-parameter --name "$SSM_PREFIX/DEFAULT_PASSWORD" --with-decryption --query "Parameter.Value" --output text)

cat > .env <<EOF
DB_USERNAME=$DB_USERNAME
DB_PASSWORD=$DB_PASSWORD
JWT_SECRET=$JWT_SECRET
DEFAULT_PASSWORD=$DEFAULT_PASSWORD
EOF
chmod 600 .env

echo "Rebuilding and restarting application..."
docker compose up -d --build --remove-orphans

echo "Removing unused Docker images..."
docker image prune -f

echo "Deployment completed."
docker compose ps