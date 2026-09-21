output "instance_id" {
  value = aws_instance.app.id
}

output "public_ip" {
  value = aws_eip.app.public_ip
}

output "domain" {
  description = "Hostname the API answers on, and the subject of its TLS certificate."
  value       = local.domain
}

output "ecr_repository_url" {
  value = aws_ecr_repository.backend.repository_url
}

output "github_deploy_role_arn" {
  value = aws_iam_role.github_deploy.arn
}

output "backup_bucket" {
  value = aws_s3_bucket.backups.id
}

output "data_volume_id" {
  value = aws_ebs_volume.data.id
}

output "smtp_user" {
  value = aws_iam_user.smtp.name
}
