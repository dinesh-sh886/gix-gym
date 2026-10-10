# ADR 0006: REST API and OpenAPI Contracts

**Status:** proposed
**Owner:** dinesh-sh886
**Date:** 2026-10-10

## Context

Services expose synchronous APIs for queries and commands. Contracts must be explicit, versioned, and validated to enable independent evolution and client generation.

## Decision

### OpenAPI 3.1 as Contract Standard

- **Every service** publishes an OpenAPI 3.1 document at `/v3/api-docs` (SpringDoc)
- **Contract-first** for shared/public APIs: specs live in `contracts/openapi/<service>.yaml`
- **Code-first** for internal/service-private endpoints: generated from Spring annotations
- **Versioning**: URL path versioning (`/api/v1/...`); spec version in `info.version`
- **Deprecation**: `deprecated: true` on operations; sunset header on responses

### Contract Location & Governance

```
contracts/openapi/
├── membership.yaml
├── facility.yaml
├── booking.yaml
├── payment.yaml
└── notification.yaml
```

- PRs changing contracts require **contract review** (breaking change check)
- `maven` plugin validates spec syntax on build (`springdoc-openapi-maven-plugin` or similar)
- Breaking changes require major version bump (`v1` → `v2` in path)

### API Design Guidelines

- **Resource-oriented** URLs: `GET /api/v1/members/{id}`, `POST /api/v1/bookings`
- **Standard errors**: RFC 7807 ProblemDetail (`application/problem+json`)
- **Pagination**: `Pageable` + `PagedModel` (Spring HATEOAS) or cursor-based
- **Filtering/Sorting**: Query parameters (`?filter=status:active&sort=createdAt,desc`)
- **Idempotency**: `Idempotency-Key` header for mutating endpoints (stored 24h)
- **Rate limiting**: `X-RateLimit-*` headers (enforced at Gateway, future)

### Client Generation

- **Angular**: `openapi-generator` → TypeScript Axios/Fetch client in `web/src/app/api/`
- **Android**: `openapi-generator` → Kotlin Retrofit client in `mobile/`
- Generated code **not committed**; generated at build time

## Consequences

### Positive
- Single source of truth for API shape
- Automated client SDKs reduce integration bugs
- Breaking change detection in CI
- Documentation always in sync

### Negative
- Contract-first adds upfront design overhead
- Generated clients may need manual wrappers for ergonomics
- SpringDoc runtime generation vs. static spec drift risk (mitigated by build-time validation)

## Verification

- `mvn verify` runs OpenAPI validation plugin
- `curl http://localhost:8081/v3/api-docs` returns valid JSON for running service
- `contracts/openapi/*.yaml` pass `swagger-codegen validate`
- Breaking change detector (e.g., `oas-diff`) runs in CI on PR

## References

- [OpenAPI 3.1 Specification](https://spec.openapis.org/oas/v3.1.0)
- [SpringDoc OpenAPI](https://springdoc.org/)
- [RFC 7807 ProblemDetail](https://datatracker.ietf.org/doc/html/rfc7807)
- ADR 0002 (Microservices), ADR 0009 (Auth & Tenant Isolation)