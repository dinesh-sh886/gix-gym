# ADR 0007: Authentication, Authorization, and Tenant Isolation

**Status:** proposed
**Owner:** dinesh-sh886
**Date:** 2026-10-10

## Context

The platform must authenticate users (members, staff, admins), authorize actions based on roles and tenancy, and enforce data isolation between organizations. Auth must work across web, mobile, and service-to-service calls.

## Decision

### Authentication: JWT (Stateless)

- **Issuer**: Auth service (future) or Keycloak (deferred); local: self-signed JWT from `api-gateway` or shared secret
- **Token format**: RS256 (asymmetric) preferred; HS256 acceptable for local dev only
- **Claims**:
  ```json
  {
    "sub": "user-uuid",
    "org_id": "org-uuid",
    "branch_id": "branch-uuid",
    "roles": ["MEMBER", "TRAINER"],
    "permissions": ["booking:read", "booking:write"],
    "iat": 1234567890,
    "exp": 1234567890
  }
  ```
- **Access token**: 15 min TTL; **Refresh token**: 30 days, rotation, stored hashed in DB
- **Token storage**: HttpOnly Secure cookie (web) / Encrypted SharedPreferences (Android)

### Authorization: RBAC + ABAC

- **Roles**: `PLATFORM_ADMIN`, `ORG_ADMIN`, `BRANCH_MANAGER`, `TRAINER`, `FRONT_DESK`, `MEMBER`
- **Permissions**: Fine-grained (`facility:read`, `booking:write`, `payment:refund`)
- **Policy enforcement**: Spring Security `@PreAuthorize` + custom `PermissionEvaluator`
- **Tenant isolation**: `OrganizationId` and `BranchId` resolved from JWT → injected into repository queries via `SecurityContext`

### Service-to-Service Auth

- **mTLS** (future, production)
- **Local**: Shared `SERVICE_JWT_SECRET` for internal calls; `X-Service-Name` header
- Services validate `aud` claim includes their service name

### Tenant Isolation Enforcement

- **Database RLS** (ADR 0004): `organization_id` / `branch_id` predicates enforced by PostgreSQL policies
- **Application layer**: `TenantContextHolder` (ThreadLocal) populated from JWT filter
- **Query interception**: Spring Data `QueryByExampleExecutor` + `Specification` auto-adds tenant predicates
- **Cross-tenant access**: Only via explicit `PlatformAdmin` permission + audit log

### Local Development

- `JWT_SECRET` in `.env` (HS256, dev only)
- Test tokens generated via script (`scripts/generate-test-token.sh`)
- No external IdP required for local dev

## Consequences

### Positive
- Stateless: no session store, scales horizontally
- JWT carries tenant context → no DB lookup for authorization
- RLS provides defense-in-depth even if app layer fails
- Standard token format works across web, mobile, services

### Negative
- Token revocation requires blocklist or short TTL (refresh token rotation mitigates)
- Key rotation needs coordination (RS256 with JWKS endpoint)
- RLS policies add migration complexity
- Service-to-service auth adds latency (mitigated by caching JWKS)

## Verification

- Unit test: `PermissionEvaluator` grants/denies based on roles + tenant
- Integration test: JWT without `org_id` → 403 on tenant-scoped endpoint
- Integration test: User from Org A receives 404 (not 403) for Org B resources
- RLS test: Direct DB query as `gix_membership` role cannot see `gix_facility` data

## References

- [Spring Security Reference](https://docs.spring.io/spring-security/reference/index.html)
- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)
- [PostgreSQL RLS](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
- ADR 0004 (Organization & Branch Tenancy), ADR 0006 (REST & OpenAPI)