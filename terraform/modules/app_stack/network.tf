data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }

  filter {
    name   = "availability-zone"
    values = [var.availability_zone]
  }
}

resource "aws_security_group" "app" {
  name        = "${var.name_prefix}-app"
  description = "Public HTTP/HTTPS for the DevHub API. Shell access is SSM Session Manager only."
  vpc_id      = data.aws_vpc.default.id
}

resource "aws_vpc_security_group_ingress_rule" "http_v4" {
  security_group_id = aws_security_group.app.id
  description       = "ACME HTTP-01 challenge and the redirect to HTTPS"
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "https_v4" {
  security_group_id = aws_security_group.app.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "https_quic_v4" {
  security_group_id = aws_security_group.app.id
  description       = "HTTP/3"
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "udp"
}

resource "aws_vpc_security_group_egress_rule" "all_v4" {
  security_group_id = aws_security_group.app.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

# The address is the DNS name and the TLS identity. Releasing it invalidates the
# issued certificate, so it is deliberately separate from the instance lifecycle.
resource "aws_eip" "app" {
  domain = "vpc"
  tags   = { Name = "${var.name_prefix}-app" }

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_eip_association" "app" {
  instance_id   = aws_instance.app.id
  allocation_id = aws_eip.app.id
}

locals {
  domain = var.custom_domain != "" ? var.custom_domain : "${replace(aws_eip.app.public_ip, ".", "-")}.sslip.io"
}
