#!/usr/bin/env bash

set -Eeuo pipefail

APP_DIR="/home/ubuntu/apartment-manager"
BRANCH="main"

cd "$APP_DIR"

echo "Downloading latest code..."
git fetch origin "$BRANCH"

echo "Updating local working copy..."
git reset --hard "origin/$BRANCH"

echo "Rebuilding and restarting application..."
docker compose up -d --build --remove-orphans

echo "Removing unused Docker images..."
docker image prune -f

echo "Deployment completed."
docker compose ps