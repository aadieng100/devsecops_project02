resource "aws_kms_key" "state_key" {
  description             = "KMS Customer Managed Key for explicit state encryption at rest"
  deletion_window_in_days = 7
  enable_key_rotation     = true

  tags = {
    Name = "${var.project_name}-state-kms-key"
  }
}

resource "aws_kms_alias" "state_key_alias" {
  name          = "alias/${var.project_name}-state-key"
  target_key_id = aws_kms_key.state_key.key_id
}
