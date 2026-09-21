module "app" {
  source = "../../modules/app_stack"

  name_prefix       = "devhub-prod"
  region            = var.region
  availability_zone = var.availability_zone
  instance_type     = var.instance_type
  custom_domain     = var.custom_domain
  acme_email        = var.acme_email
  github_repo       = var.github_repo
  parameter_path    = "/devhub/prod"
}
