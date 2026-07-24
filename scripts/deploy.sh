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

EMAIL_FETCH_ENABLED=$(aws ssm get-parameter --name "$SSM_PREFIX/EMAIL_FETCH_ENABLED" --with-decryption --query "Parameter.Value" --output text 2>/dev/null || echo "false")
EMAIL_FETCH_ADDRESS=$(aws ssm get-parameter --name "$SSM_PREFIX/EMAIL_FETCH_ADDRESS" --with-decryption --query "Parameter.Value" --output text 2>/dev/null || echo "")
EMAIL_FETCH_PASSWORD=$(aws ssm get-parameter --name "$SSM_PREFIX/EMAIL_FETCH_PASSWORD" --with-decryption --query "Parameter.Value" --output text 2>/dev/null || echo "")

cat > .env <<EOF
DB_USERNAME=$DB_USERNAME
DB_PASSWORD=$DB_PASSWORD
JWT_SECRET=$JWT_SECRET
DEFAULT_PASSWORD=$DEFAULT_PASSWORD
EMAIL_FETCH_ENABLED=$EMAIL_FETCH_ENABLED
EMAIL_FETCH_ADDRESS=$EMAIL_FETCH_ADDRESS
EMAIL_FETCH_PASSWORD=$EMAIL_FETCH_PASSWORD
EOF
chmod 600 .env

echo "Rebuilding and restarting application..."
docker compose up -d --build --remove-orphans

echo "Resetting default users (truncating users table)..."
docker compose exec -T db psql -U "$DB_USERNAME" -d apartment-management-db -c "TRUNCATE TABLE users CASCADE;" || echo "Warning: Could not truncate users table (DB may not be ready yet)"

echo "Restarting backend to re-seed default users..."
docker compose restart backend

echo "Removing unused Docker images..."
docker image prune -f

echo "Deployment completed."
docker compose ps