# ADR 0003: Database per Service

**Status:** proposed
**Owner:** dinesh-sh886
**Date:** 2026-10-10

## Context

Microservices require loose coupling. Shared databases create tight schema coupling, impede independent deployment, and cause contention. Each service must own its data exclusively.

## Decision

**Each service owns a dedicated PostgreSQL database** with a least-privilege role.

### Local Development

- Single shared PostgreSQL instance (Docker Compose) for resource efficiency
- **Distinct databases** per service: `gix_membership`, `gix_facility`, `gix_booking`, `gix_payment`, `gix_notification`
- **Distinct roles** per service: `gix_membership`, `gix_facility`, etc., with `LOGIN` and `CONNECT` only to their own database
- No cross-database queries or foreign keys
- Superuser (`postgres`) used only for provisioning (initialization scripts), not by services at runtime

### Provisioning

- Database and role creation via `init-sql` scripts mounted in PostgreSQL container (run once on empty volume)
- Service credentials injected via environment variables (`.env`, not committed)
- Flyway migrations managed **per service** in `services/<name>/src/main/resources/db/migration/`

### Production (Deferred)

- Managed PostgreSQL per service (e.g., RDS instances) — see ADR 0010 (AWS Deferred)

## Consequences

### Positive
- True data ownership: schema changes don't affect other services
- Independent backup, restore, and scaling per service
- Clear security boundary: compromised service credentials don't expose other services' data
- Aligns with microservices principle of high cohesion, loose coupling

### Negative
- Cross-service queries require API composition or event-driven materialized views
- Distributed transactions need saga pattern or eventual consistency (see ADR 0005)
- More databases to provision, monitor, and backup (mitigated locally by single instance)

## Verification

- `docker compose exec postgres psql -U gix_membership -d gix_membership -c '\dt'` succeeds
- `docker compose exec postgres psql -U gix_membership -d gix_facility -c '\dt'` fails (permission denied)
- Each service's Flyway migrations apply cleanly to its own database

## References

- [Database per Service Pattern](https://microservices.io/patterns/data/database-per-service.html)
- ADR 0004 (Organization & Branch Tenancy), ADR 0005 (Kafka Event Integration)
- `infrastructure/docker/compose.yml` for local provisioning