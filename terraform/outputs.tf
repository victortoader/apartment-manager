output "instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.app.id
}

output "public_ip" {
  description = "Public IP address"
  value       = aws_instance.app.public_ip
}

output "public_dns" {
  description = "Public DNS name"
  value       = aws_instance.app.public_dns
}

output "app_url" {
  description = "Application URL"
  value       = "http://${aws_instance.app.public_ip}"
}

output "ssh_command" {
  description = "SSH command to connect to the instance (only available if ssh_public_key was provided)"
  value       = var.ssh_public_key != "" ? "ssh -i <your-key>.pem ubuntu@${aws_instance.app.public_ip}" : "SSH not configured - use SSM Session Manager or AWS Console"
}

output "ssm_prefix" {
  description = "SSM parameter prefix"
  value       = "/${var.ssm_prefix}"
}

output "uploads_backup_bucket" {
  description = "S3 bucket for daily uploads backups"
  value       = aws_s3_bucket.uploads_backup.id
}

output "db_backup_bucket" {
  description = "S3 bucket for daily DB backups"
  value       = aws_s3_bucket.db_backup.id
}
