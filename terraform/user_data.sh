#!/bin/bash
set -euxo pipefail

export DEBIAN_FRONTEND=noninteractive

echo "=== Updating system packages ==="
apt-get update
apt-get upgrade -y

echo "=== Creating swap file ==="
fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab

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

echo "=== Setting up cron jobs for backups ==="
cat > /etc/cron.d/apartment-manager-backups << 'CRONEOF'
0 2 * * * ubuntu cd /home/ubuntu/apartment-manager && bash scripts/backup-db.sh >> /var/log/db-backup.log 2>&1
0 3 * * * ubuntu cd /home/ubuntu/apartment-manager && bash scripts/backup-uploads.sh >> /var/log/uploads-backup.log 2>&1
CRONEOF
chmod 644 /etc/cron.d/apartment-manager-backups

echo "=== Setup complete ==="
