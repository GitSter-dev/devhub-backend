// SES identity and the IAM user whose credentials become SMTP credentials.
// The access key itself is created out of band so the secret never reaches
// Terraform state; see the runbook in deploy/RESTORE.md.

resource "aws_sesv2_email_identity" "sender" {
  count          = var.ses_identity != "" ? 1 : 0
  email_identity = var.ses_identity
}

resource "aws_iam_user" "smtp" {
  name = "${var.name_prefix}-smtp"
  path = "/service/"
}

data "aws_iam_policy_document" "smtp" {
  statement {
    sid       = "SendMail"
    actions   = ["ses:SendRawEmail"]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "ses:FromAddress"
      values   = [var.ses_identity != "" ? var.ses_identity : "*"]
    }
  }
}

resource "aws_iam_user_policy" "smtp" {
  name   = "send-mail"
  user   = aws_iam_user.smtp.name
  policy = data.aws_iam_policy_document.smtp.json
}
