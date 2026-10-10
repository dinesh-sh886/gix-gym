# ADR 0004: Organization and Branch Tenancy

**Status:** proposed
**Owner:** dinesh-sh886
**Date:** 2026-10-10

## Context

The SaaS platform must support multiple gym organizations (tenants), each potentially operating multiple branches (locations). Data isolation, customization, and regulatory compliance require a clear tenancy model from the start.

## Decision

**Hierarchical multi-tenancy: Organization → Branch**

### Tenancy Model

- **Organization** (Tenant): Top-level billing and administrative unit. Owns subscription, branding, global settings.
- **Branch** (Location): Physical gym location within an organization. Owns facilities, schedules, local staff.
- **Membership** belongs to an Organization; check-ins occur at a Branch.
- **Booking** is scoped to a Branch (class at a location).
- **Payment** is at Organization level (subscription billing).

### Technical Implementation

- **Shared database, discriminator column** (`organization_id`, `branch_id`) on all tenant-scoped tables
- **Row-Level Security (RLS)** policies in PostgreSQL enforce isolation at the database level
- **JWT claims** carry `org_id` and `branch_id`; Spring Security filters enforce access
- **No cross-tenant queries** — every repository query includes tenant predicates
- **Super-admin** role (platform operators) can query across tenants via explicit opt-in

### Branch Context Resolution

- HTTP header `X-Branch-Id` or subdomain (`branch1.gixgym.com`) resolves branch context
- Fallback to user's default branch from profile
- API Gateway (future) validates branch belongs to JWT's organization

### Provisioning

- Flyway migration creates `organization`, `branch` tables and RLS policies
- New organization onboarding: insert org + default branch + admin user in single transaction

## Consequences

### Positive
- Strong data isolation at DB level (defense in depth)
- Supports multi-branch gyms naturally
- JWT-based context propagation works with stateless services
- RLS policies survive application bugs (DB-enforced)

### Negative
- Every query must include tenant predicates (mitigated by Spring Data spec/QueryDSL helpers)
- Schema migrations must maintain RLS policies
- Reporting across tenants requires explicit platform-level access

## Verification

- RLS policy blocks `SELECT * FROM member WHERE organization_id != current_setting('app.current_org')`
- Integration test: User from Org A cannot read Branch B's bookings
- JWT with `org_id=1, branch_id=2` rejects access to `branch_id=3` resources

## References

- [PostgreSQL Row-Level Security](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
- [Multi-Tenancy Patterns](https://learn.microsoft.com/en-us/azure/architecture/guide/multitenant/overview)
- ADR 0003 (Database per Service), ADR 0009 (Auth & Tenant Isolation)