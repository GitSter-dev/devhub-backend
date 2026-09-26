# ---------------------------------------------------------------------------
# Moderator console: a static SPA served by its own container behind Caddy at
# /console. It ships from a separate repo with its own, narrower deploy role.
# ---------------------------------------------------------------------------

resource "aws_ecr_repository" "console" {
  name                 = "${var.name_prefix}-console"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }
}

resource "aws_ecr_lifecycle_policy" "console" {
  repository = aws_ecr_repository.console.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep the 20 most recent images so a rollback target always exists."
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 20
      }
      action = { type = "expire" }
    }]
  })
}

# The console repo may roll the console and nothing else. A frontend build pulls
# a large npm tree, so its role gets this fixed command, not a root shell on the
# host that also runs Postgres.
resource "aws_ssm_document" "deploy_console" {
  name          = "${var.name_prefix}-deploy-console"
  document_type = "Command"

  content = jsonencode({
    schemaVersion = "2.2"
    description   = "Roll the moderator console to an image tag."
    parameters = {
      imageTag = {
        type           = "String"
        allowedPattern = "^[0-9a-f]{40}$"
      }
    }
    mainSteps = [{
      action = "aws:runShellScript"
      name   = "deployConsole"
      inputs = {
        timeoutSeconds = "300"
        runCommand     = ["AWS_REGION=${var.region} /opt/devhub/deploy-console.sh {{ imageTag }}"]
      }
    }]
  })
}

locals {
  console_repo_enabled = var.console_github_repo != ""
  console_repo_owner   = local.console_repo_enabled ? split("/", var.console_github_repo)[0] : ""
  console_repo_name    = local.console_repo_enabled ? split("/", var.console_github_repo)[1] : ""
}

data "aws_iam_policy_document" "github_console_assume_role" {
  count = local.console_repo_enabled ? 1 : 0

  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [data.aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values = compact([
        "repo:${var.console_github_repo}:environment:${var.github_environment}",
        var.github_owner_id != "" && var.console_github_repo_id != "" ?
        "repo:${local.console_repo_owner}@${var.github_owner_id}/${local.console_repo_name}@${var.console_github_repo_id}:environment:${var.github_environment}" : "",
      ])
    }
  }
}

resource "aws_iam_role" "github_console_deploy" {
  count = local.console_repo_enabled ? 1 : 0

  name               = "${var.name_prefix}-github-console-deploy"
  description        = "Assumed by the console repo's GitHub Actions to push its image and roll it."
  assume_role_policy = data.aws_iam_policy_document.github_console_assume_role[0].json
}

data "aws_iam_policy_document" "github_console_deploy" {
  statement {
    sid       = "EcrAuth"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "PushImage"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:CompleteLayerUpload",
      "ecr:DescribeImages",
      "ecr:GetDownloadUrlForLayer",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
    ]
    resources = [aws_ecr_repository.console.arn]
  }

  statement {
    sid     = "RollConsole"
    actions = ["ssm:SendCommand"]
    resources = [
      "arn:aws:ec2:${var.region}:${local.account_id}:instance/${aws_instance.app.id}",
      aws_ssm_document.deploy_console.arn,
    ]
  }

  statement {
    sid       = "ReadCommandResult"
    actions   = ["ssm:GetCommandInvocation", "ssm:ListCommandInvocations"]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "github_console_deploy" {
  count = local.console_repo_enabled ? 1 : 0

  name   = "deploy"
  role   = aws_iam_role.github_console_deploy[0].id
  policy = data.aws_iam_policy_document.github_console_deploy.json
}
