# ADR 0010: AWS Deferred Until Release Stage

**Status:** proposed
**Owner:** dinesh-sh886
**Date:** 2026-10-10

## Context

The project will eventually deploy to AWS (ECS, RDS, MSK, ALB, etc.). However, local development, testing, and early CI must **not** require AWS credentials, accounts, or paid services.

## Decision

**AWS is explicitly prohibited from local development and CI pipelines until a dedicated release-stage task authorizes it.**

### Prohibited in Local/CI

- AWS SDK calls to real services (S3, DynamoDB, Secrets Manager, etc.)
- `terraform apply` or `plan` against real AWS account
- IAM roles, instance profiles, or STS assume-role in local code
- CloudFormation/CDK synthesis with real AWS endpoints
- ECS/ECR pushes, RDS connections, MSK connections
- Any environment variable prefixed `AWS_` or `AWS_DEFAULT_` in local `.env`

### Allowed Local Substitutes

| AWS Service | Local Substitute |
|-------------|------------------|
| RDS (PostgreSQL) | Docker Compose PostgreSQL (ADR 0008) |
| MSK (Kafka) | Docker Compose Kafka (KRaft) (ADR 0008) |
| S3 | MinIO (future, if needed) or local filesystem |
| Secrets Manager | `.env` file (local), GitHub Actions secrets (CI) |
| ALB / API Gateway | Local API Gateway service (Spring Cloud Gateway) or direct service ports |
| ECS / Fargate | `docker compose` or local `java -jar` |
| CloudWatch / X-Ray | Micrometer + Prometheus + Grafana (local stack, future) |

### Release-Stage Authorization

- A **separate, explicitly approved task** (e.g., `P02-RELEASE-AWS`) will introduce:
  - Terraform modules for AWS resources
  - GitHub Actions `aws-actions/configure-aws-credentials` with OIDC
  - Service-specific AWS integration tests (opt-in, `@Tag("aws")`)
  - Production deployment pipelines
- Until then, **no AWS code, config, or credentials** enter the repository

### Enforcement

- **Pre-commit hook** (future): `trufflehog` / `gitleaks` scans for AWS keys
- **CI job**: `mvn verify -DskipAwsTests` (default); AWS tests only run on `release/*` branches
- **Code review checklist**: "Does this change introduce AWS dependencies?"

## Consequences

### Positive
- Zero-cost local development
- No credential leakage risk
- CI runs anywhere (GitHub Actions, local, self-hosted) without AWS setup
- Clear separation: "local-first" vs. "cloud-deployed" concerns

### Negative
- Local-parity gaps (e.g., IAM auth, RDS Proxy, MSK IAM) — documented in runbooks
- Delayed discovery of AWS-specific integration issues
- Requires discipline to not leak AWS assumptions into core code

## Verification

- `grep -r "aws\|AWS" --include="*.java" --include="*.yml" --include="*.yaml" --include="*.xml" .` returns only ADR references and comments
- `mvn verify` passes without `AWS_*` env vars set
- No `software.amazon.awssdk` or `com.amazonaws` dependencies in `pom.xml` (except `test` scope for LocalStack, if used)
- CI workflow has no `aws-actions/*` steps

## References

- ADR 0008 (Local Docker Compose)
- [12-Factor App: Config](https://12factor.net/config)
- [GitHub Actions OIDC for AWS](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services)