# ADR 0002: Microservices and Independent Deployment

**Status:** proposed
**Owner:** dinesh-sh886
**Date:** 2026-10-10

## Context

The Gym Management SaaS must support multiple bounded contexts (membership, facility, booking, payment, notification) with independent scaling, deployment, and team ownership. A monolithic deployment would couple release cycles and limit scalability.

## Decision

Adopt a **microservices architecture** with the following principles:

1. **One service per bounded context** — each service owns a single business capability
2. **Independent deployability** — each service is a separate Spring Boot JAR, built and deployed independently
3. **Private data ownership** — no shared databases; each service has its own PostgreSQL database
4. **Async communication by default** — Kafka events for cross-service communication; REST only for synchronous queries/commands
5. **Explicit contracts** — OpenAPI for REST, AsyncAPI for Kafka events, versioned in `contracts/`
6. **Monorepo for now** — all services in one Git repository (`gix-gym`) with Maven multi-module structure; can split to polyrepo later if needed

### Planned Services

| Service | Responsibility | Database |
|---------|---------------|----------|
| `membership` | Members, profiles, membership plans, check-ins | `gix_membership` |
| `facility` | Gym locations, rooms, equipment, schedules | `gix_facility` |
| `booking` | Class bookings, personal training, waitlists | `gix_booking` |
| `payment` | Invoices, subscriptions, payment processing, refunds | `gix_payment` |
| `notification` | Email, push, in-app notifications, templates | `gix_notification` |
| `api-gateway` | (Future) Edge routing, auth validation, rate limiting | — |

Services will be scaffolded incrementally (P01.04), not all at once.

## Consequences

### Positive
- Teams can develop, test, and deploy services independently
- Failure isolation: one service outage doesn't cascade (with proper circuit breakers)
- Technology heterogeneity possible per service (though Java/Spring is standard)
- Scales with organization growth

### Negative
- Operational complexity: more services to monitor, deploy, debug
- Distributed system challenges: eventual consistency, saga patterns, idempotency
- Network latency and serialization overhead vs. in-process calls
- Requires disciplined contract management and versioning

## Verification

- Each service builds independently: `mvn -pl services/<name> verify`
- Services start independently on distinct ports
- Contract tests validate compatibility between producers/consumers

## References

- [Microservices Pattern](https://microservices.io/patterns/microservices.html)
- [Spring Cloud](https://spring.io/projects/spring-cloud)
- ADR 0003 (Database per Service), ADR 0005 (Kafka Event Integration)