provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project   = "devhub"
      Env       = "prod"
      ManagedBy = "terraform"
    }
  }
}
