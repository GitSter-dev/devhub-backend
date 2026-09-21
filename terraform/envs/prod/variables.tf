variable "region" {
  type    = string
  default = "eu-north-1"
}

variable "availability_zone" {
  type    = string
  default = "eu-north-1a"
}

variable "instance_type" {
  type    = string
  default = "c7i-flex.large"
}

variable "custom_domain" {
  description = "Set once a real domain exists; empty derives an sslip.io name from the Elastic IP."
  type        = string
  default     = ""
}

variable "acme_email" {
  type = string
}

variable "github_repo" {
  type = string
}

variable "github_owner_id" {
  type    = string
  default = ""
}

variable "github_repo_id" {
  type    = string
  default = ""
}
