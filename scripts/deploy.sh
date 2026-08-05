#!/usr/bin/env bash

set -Eeuo pipefail

APP_DIR="/home/ubuntu/apartment-manager"
BRANCH="${1:-}"
if [ -z "$BRANCH" ]; then
  if read -r -t 10 -p "Branch to deploy [main]: " BRANCH; then
    BRANCH="${BRANCH:-main}"
  else
    echo "No input received, deploying main by default"
    BRANCH="main"
  fi
fi
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
EMAIL_NOTIFY_ENABLED=$(aws ssm get-parameter --name "$SSM_PREFIX/EMAIL_NOTIFY_ENABLED" --with-decryption --query "Parameter.Value" --output text 2>/dev/null || echo "false")
EMAIL_NOTIFY_FROM=$(aws ssm get-parameter --name "$SSM_PREFIX/EMAIL_NOTIFY_FROM" --with-decryption --query "Parameter.Value" --output text 2>/dev/null || echo "")
APP_BASE_URL=$(aws ssm get-parameter --name "$SSM_PREFIX/APP_BASE_URL" --with-decryption --query "Parameter.Value" --output text 2>/dev/null || echo "https://apartmentmanager.jake.eu")

cat > .env <<EOF
DB_USERNAME=$DB_USERNAME
DB_PASSWORD=$DB_PASSWORD
JWT_SECRET=$JWT_SECRET
DEFAULT_PASSWORD=$DEFAULT_PASSWORD
EMAIL_FETCH_ENABLED=$EMAIL_FETCH_ENABLED
EMAIL_FETCH_ADDRESS=$EMAIL_FETCH_ADDRESS
EMAIL_FETCH_PASSWORD=$EMAIL_FETCH_PASSWORD
EMAIL_NOTIFY_ENABLED=$EMAIL_NOTIFY_ENABLED
EMAIL_NOTIFY_FROM=$EMAIL_NOTIFY_FROM
APP_BASE_URL=$APP_BASE_URL
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