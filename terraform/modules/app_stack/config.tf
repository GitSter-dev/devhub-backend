// Non-secret runtime configuration. Secrets (DB_PASSWORD, JWT_PRIVATE_KEY,
// JWT_PUBLIC_KEY, FIREBASE_CREDENTIALS_JSON) are deliberately NOT managed here:
// they are written once with `aws ssm put-parameter` so no plaintext ever
// reaches Terraform state. The instance policy grants on the path wildcard, so
// it can read parameters Terraform has never seen.

locals {
  config = {
    DB_HOST            = "postgres"
    DB_PORT            = "5432"
    DB_NAME            = var.db_name
    DB_USER            = var.db_user
    ECR_REPOSITORY_URL = aws_ecr_repository.backend.repository_url

    DEVHUB_DOMAIN = local.domain
    ACME_EMAIL    = var.acme_email
    BACKUP_BUCKET = aws_s3_bucket.backups.id

    # Caddy terminates TLS and sets X-Forwarded-*; without this the app sees
    # Caddy's container address as the client for every request.
    FORWARD_HEADERS_STRATEGY = "framework"

    # The OpenAPI routes are permitAll, so leaving this on publishes the whole
    # API surface, admin routes included, to anonymous callers.
    API_DOCS_ENABLED = "false"

    REALTIME_ALLOWED_ORIGINS = "https://${local.domain}"

    MAIL_HOST = "email-smtp.${var.region}.amazonaws.com"
    MAIL_PORT = "587"
    MAIL_FROM = var.mail_from

    JWT_ISSUER               = "devhub"
    JWT_PUBLIC_KEY_LOCATION  = "file:/run/secrets/public.pem"
    JWT_PRIVATE_KEY_LOCATION = "file:/run/secrets/private.pem"

    PUSH_ENABLED         = "false"
    FIREBASE_CREDENTIALS = "/run/secrets/firebase.json"

    SCHEDULING_ENABLED = "true"
  }
}

resource "aws_ssm_parameter" "config" {
  for_each = local.config

  name  = "${var.parameter_path}/${each.key}"
  type  = "String"
  value = each.value
}
