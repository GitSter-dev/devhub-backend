output "instance_id" { value = module.app.instance_id }
output "public_ip" { value = module.app.public_ip }
output "domain" { value = module.app.domain }
output "ecr_repository_url" { value = module.app.ecr_repository_url }
output "github_deploy_role_arn" { value = module.app.github_deploy_role_arn }
output "backup_bucket" { value = module.app.backup_bucket }
output "smtp_user" { value = module.app.smtp_user }
