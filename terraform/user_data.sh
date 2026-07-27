#!/bin/bash
set -euxo pipefail

export DEBIAN_FRONTEND=noninteractive

echo "=== Updating system packages ==="
apt-get update
apt-get upgrade -y

echo "=== Installing dependencies ==="
apt-get install -y ca-certificates curl gnupg git unzip

echo "=== Installing Docker ==="
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable docker
systemctl start docker
usermod -aG docker ubuntu

echo "=== Installing AWS CLI ==="
curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "/tmp/awscliv2.zip"
unzip -q /tmp/awscliv2.zip -d /tmp
/tmp/aws/install
rm -rf /tmp/aws /tmp/awscliv2.zip

echo "=== Cloning repository ==="
su - ubuntu -c "git clone --branch ${branch} ${git_repo_url} /home/ubuntu/apartment-manager"

echo "=== Preparing for HTTP-only deployment (no SSL) ==="
APP_DIR="/home/ubuntu/apartment-manager"
cp "$APP_DIR/nginx/default.local.conf" "$APP_DIR/nginx/default.conf"
sed -i '/\/etc\/letsencrypt/d' "$APP_DIR/docker-compose.yml"

echo "=== Running deployment ==="
su - ubuntu -c "cd $APP_DIR && bash scripts/deploy-test.sh"

echo "=== Setup complete ==="
