# Gix Gym — Gym Management SaaS

A microservices-based Gym Management Software-as-a-Service platform built with Java 21, Spring Boot, Angular, and Android.

## Ownership Convention

All project-authored source files, documentation, and configuration files include a header attributing ownership to the project owner: **dinesh-sh886**.

Generated code, dependency lockfiles, binary files, vendor files, and third-party source are **not** modified to add ownership text.

## Architecture Overview

- **Backend**: Java 21 LTS, Spring Boot, Maven (multi-module monorepo)
- **Messaging**: Apache Kafka for async event-driven communication
- **Databases**: PostgreSQL per service (local: shared instance, distinct databases/roles)
- **API Contracts**: OpenAPI 3.x for REST, AsyncAPI for Kafka events
- **Frontend**: Angular web application
- **Mobile**: Native Android (Kotlin)
- **Local Infrastructure**: Docker Compose (PostgreSQL, Kafka)
- **CI**: GitHub Actions

AWS resources are explicitly **deferred** until a release-stage task authorizes them. Local development and tests do not require AWS credentials.

## Project Structure

```
gix-gym/
├── contracts/           # OpenAPI & Kafka event contracts
├── docs/                # Architecture, ADRs, runbooks, status
├── infrastructure/      # Docker Compose, local infra configs
├── scripts/             # Utility scripts
├── services/            # Spring Boot microservices (to be added)
├── web/                 # Angular frontend (to be added)
├── mobile/              # Android app (to be added)
├── pom.xml              # Maven parent POM
├── .github/workflows/   # CI pipelines
└── .env.example         # Environment variable template
```

## Getting Started

### Prerequisites

- Java 21 LTS
- Maven 3.9+
- Node.js 20+ and npm
- Docker Desktop with WSL 2 integration (for PostgreSQL & Kafka)
- Android Studio / Android SDK (for mobile development)

### Local Development

```bash
# Start infrastructure
docker compose -f infrastructure/docker/compose.yml up -d

# Build all services
mvn verify

# Run a specific service
cd services/<service-name> && mvn spring-boot:run
```

## License

License decision: **pending**. A `LICENSE` file will be added once the project's license is established.

## Status

See [P01 Status Register](docs/status/P01-status.md) for current development baseline progress.