terraform {
  required_version = ">= 1.11"

  backend "s3" {
    bucket = "devhub-tfstate-163570183808"
    key    = "backend/prod/terraform.tfstate"
    # The bucket predates this stack and lives in eu-central-1; the resources
    # themselves are in eu-north-1.
    region = "eu-central-1"

    encrypt = true
    # S3-native conditional-write locking. The dynamodb_table argument has been
    # deprecated since Terraform 1.11.
    use_lockfile = true
  }
}
