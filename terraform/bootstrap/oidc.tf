# 1. Dynamically retrieve the TLS properties of GitHub's secure token authority
data "tls_certificate" "github" {
  url = "https://token.actions.githubusercontent.com"
}

# 2. Establish the Federated OpenID Connect Provider for GitHub Actions
resource "aws_iam_openid_connect_provider" "github" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.github.certificates[0].sha1_fingerprint]
}

# 3. Create the Dedicated IAM Execution Role for the CI/CD Pipeline
resource "aws_iam_role" "github_actions_role" {
  name        = "${var.project_name}-github-actions-role"
  description = "Short-lived execution identity for devsecops_project02 GitHub Actions pipelines"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Federated = aws_iam_openid_connect_provider.github.arn
        }
        Action = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringEquals = {
            "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
          }
          StringLike = {
            "token.actions.githubusercontent.com:sub" = "repo:aadieng100/devsecops_project02:*"
          }
        }
      }
    ]
  })
}

# 4. Attach Permissions to Allow the IaC Engine to Provision Infrastructure
resource "aws_iam_role_policy_attachment" "gha_admin_attach" {
  role       = aws_iam_role.github_actions_role.name
  policy_arn = "arn:aws:iam::aws:policy/AdministratorAccess"
}
