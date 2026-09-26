variable "name_prefix" {
  description = "Prefix for every resource name, e.g. devhub-prod."
  type        = string
}

variable "region" {
  description = "Region the stack runs in."
  type        = string
}

variable "availability_zone" {
  description = "AZ for the instance and its data volume. They must match."
  type        = string
}

variable "instance_type" {
  type    = string
  default = "c7i-flex.large"
}

variable "cpu_credits" {
  description = "Burst credit mode. Only applied to T-family instance types."
  type        = string
  default     = "standard"
}

variable "root_volume_size" {
  type    = number
  default = 20
}

variable "data_volume_size" {
  type    = number
  default = 20
}

variable "custom_domain" {
  description = "Real hostname for the API. Empty derives <eip-with-dashes>.sslip.io from the Elastic IP."
  type        = string
  default     = ""
}

variable "acme_email" {
  description = "Contact address Let's Encrypt uses for expiry notices."
  type        = string
}

variable "github_repo" {
  description = "owner/name of the repo allowed to assume the deploy role."
  type        = string
}

variable "github_environment" {
  description = "GitHub Environment the deploy job runs in. Scopes the OIDC trust more tightly than a branch ref."
  type        = string
  default     = "production"
}

variable "parameter_path" {
  description = "SSM Parameter Store prefix holding this stack's configuration and secrets."
  type        = string
  default     = "/devhub/prod"
}

variable "db_name" {
  type    = string
  default = "devhub"
}

variable "db_user" {
  type    = string
  default = "devhub"
}

variable "mail_from" {
  type    = string
  default = "DevHub <no-reply@devhub.local>"
}

variable "backup_retention_days" {
  description = "How long nightly pg_dump output is kept in S3."
  type        = number
  default     = 30
}

variable "snapshot_retention_count" {
  description = "How many daily EBS snapshots of the data volume to keep."
  type        = number
  default     = 7
}

variable "github_owner_id" {
  description = "Numeric GitHub account id, used for the ID-qualified OIDC subject. Empty trusts only the plain subject."
  type        = string
  default     = ""
}

variable "github_repo_id" {
  description = "Numeric GitHub repository id, used for the ID-qualified OIDC subject."
  type        = string
  default     = ""
}

variable "ses_identity" {
  description = "Email address or domain verified with SES and used as the sender. Empty skips identity creation."
  type        = string
  default     = ""
}

variable "console_github_repo" {
  description = "owner/name of the console repo allowed to assume the console deploy role. Empty creates no role."
  type        = string
  default     = ""
}

variable "console_github_repo_id" {
  description = "Numeric GitHub repository id of the console repo, used for the ID-qualified OIDC subject."
  type        = string
  default     = ""
}
