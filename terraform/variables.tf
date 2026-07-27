variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "eu-central-1"
}

variable "project_name" {
  description = "Project name used for resource naming and SSM prefix"
  type        = string
  default     = "apartment-manager"
}

variable "environment" {
  description = "Environment name (e.g. prod, test)"
  type        = string
  default     = "test"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.small"
}

variable "volume_size" {
  description = "Root volume size in GB"
  type        = number
  default     = 30
}

variable "ssh_public_key" {
  description = "SSH public key for the deployer key pair (leave empty to skip)"
  type        = string
  default     = ""
}

variable "ssh_cidr" {
  description = "CIDR block for SSH access"
  type        = string
  default     = "0.0.0.0/0"
}

variable "git_repo_url" {
  description = "Git repository URL"
  type        = string
  default     = "https://github.com/victortoader/apartment-manager.git"
}

variable "branch" {
  description = "Git branch to deploy"
  type        = string
  default     = "main"
}
