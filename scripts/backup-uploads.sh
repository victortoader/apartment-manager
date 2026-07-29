#!/usr/bin/env bash
set -Eeuo pipefail

BUCKET="apartment-manager-uploads-backups"
DATE=$(date +%Y-%m-%d)

docker run --rm -v apartment-manager_uploads:/uploads alpine \
  tar czf - -C /uploads . | \
  aws s3 cp - "s3://$BUCKET/uploads-$DATE.tar.gz" --no-progress
