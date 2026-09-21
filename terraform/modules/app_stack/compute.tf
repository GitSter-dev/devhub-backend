data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-6.1-x86_64"
}

resource "aws_ebs_volume" "data" {
  availability_zone = var.availability_zone
  size              = var.data_volume_size
  type              = "gp3"
  encrypted         = true

  tags = {
    Name   = "${var.name_prefix}-data"
    Backup = "daily"
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_instance" "app" {
  ami                    = data.aws_ssm_parameter.al2023.value
  instance_type          = var.instance_type
  availability_zone      = var.availability_zone
  subnet_id              = data.aws_subnets.default.ids[0]
  vpc_security_group_ids = [aws_security_group.app.id]
  iam_instance_profile   = aws_iam_instance_profile.instance.name

  # Only the T family exposes burst credits; the flex families manage their own.
  dynamic "credit_specification" {
    for_each = startswith(var.instance_type, "t") ? [1] : []
    content {
      cpu_credits = var.cpu_credits
    }
  }

  metadata_options {
    http_tokens   = "required"
    http_endpoint = "enabled"
  }

  root_block_device {
    volume_type           = "gp3"
    volume_size           = var.root_volume_size
    encrypted             = true
    delete_on_termination = true
  }

  user_data = templatefile("${path.module}/templates/cloud-init.yaml.tftpl", {
    region          = var.region
    parameter_path  = var.parameter_path
    volume_id       = aws_ebs_volume.data.id
    compose_version = "v2.40.3"
  })

  # user_data is bootstrap only. Everything that changes over time ships through
  # the deploy workflow, so an edit here must not recycle a running box.
  user_data_replace_on_change = false

  tags = { Name = var.name_prefix }

  depends_on = [aws_iam_role_policy.instance]
}

resource "aws_volume_attachment" "data" {
  device_name = "/dev/sdf"
  volume_id   = aws_ebs_volume.data.id
  instance_id = aws_instance.app.id
}
