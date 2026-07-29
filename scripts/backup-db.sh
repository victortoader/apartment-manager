#!/usr/bin/env bash
set -Eeuo pipefail

BUCKET="apartment-manager-db-backups"
DATE=$(date +%Y-%m-%d)
SSM_PREFIX="/apartment-manager"

DB_USERNAME=$(aws ssm get-parameter --name "$SSM_PREFIX/DB_USERNAME" --with-decryption --query "Parameter.Value" --output text)
DB_PASSWORD=$(aws ssm get-parameter --name "$SSM_PREFIX/DB_PASSWORD" --with-decryption --query "Parameter.Value" --output text)

cd /home/ubuntu/apartment-manager

docker compose exec -T db \
  pg_dump -U "$DB_USERNAME" apartment-management-db | gzip | \
  aws s3 cp - "s3://$BUCKET/apartment-db-$DATE.sql.gz" --no-progress
