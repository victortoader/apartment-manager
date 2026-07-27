output "instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.app.id
}

output "public_ip" {
  description = "Public IP address"
  value       = aws_eip.app.public_ip
}

output "public_dns" {
  description = "Public DNS name"
  value       = aws_eip.app.public_dns
}

output "app_url" {
  description = "Application URL"
  value       = "http://${aws_eip.app.public_ip}"
}

output "ssh_command" {
  description = "SSH command to connect to the instance (only available if ssh_public_key was provided)"
  value       = var.ssh_public_key != "" ? "ssh -i <your-key>.pem ubuntu@${aws_eip.app.public_ip}" : "SSH not configured - use SSM Session Manager or AWS Console"
}

output "ssm_prefix" {
  description = "SSM parameter prefix"
  value       = "/${var.project_name}"
}
