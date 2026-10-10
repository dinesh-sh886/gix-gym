# ADR 0008: Local Docker Compose Development

**Status:** proposed
**Owner:** dinesh-sh886
**Date:** 2026-10-10

## Context

Developers need a reproducible, zero-config local environment for PostgreSQL and Kafka. Docker Compose provides this without cloud dependencies.

## Decision

### Compose Stack (`infrastructure/docker/compose.yml`)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${POSTGRES_SUPERUSER_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-sql:/docker-entrypoint-initdb.d:ro
    ports:
      - "127.0.0.1:5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 3s
      retries: 10

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_LOG_DIRS: /var/lib/kafka/data
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    volumes:
      - kafka_data:/var/lib/kafka/data
    ports:
      - "127.0.0.1:9092:9092"
      - "127.0.0.1:9093:9093"
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092"]
      interval: 10s
      timeout: 5s
      retries: 10

  schema-registry:
    image: confluentinc/cp-schema-registry:7.6.0
    environment:
      SCHEMA_REGISTRY_HOST_NAME: schema-registry
      SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: PLAINTEXT://kafka:9092
      SCHEMA_REGISTRY_LISTENERS: http://0.0.0.0:8081
    ports:
      - "127.0.0.1:8081:8081"
    depends_on:
      kafka:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/subjects"]
      interval: 10s
      timeout: 5s
      retries: 10

volumes:
  postgres_data:
  kafka_data:
```

### Key Requirements

- **Pinned image versions** (e.g., `postgres:16-alpine`, `cp-kafka:7.6.0`)
- **Health checks** on all services
- **Persistent volumes** (`postgres_data`, `kafka_data`)
- **Localhost binding** (`127.0.0.1:port`) — no external exposure
- **Credentials via env vars** (`.env` file, not committed)
- **Distinct ports**: 5432 (PG), 9092/9093 (Kafka), 8081 (Schema Registry)
- **Init scripts** in `infrastructure/docker/init-sql/` create databases & roles (run once on empty volume)
- **Schema Registry optional** — can be disabled by commenting out if it blocks progress

### Database Initialization (`infrastructure/docker/init-sql/01-provision.sql`)

```sql
-- Run once on empty volume
CREATE DATABASE gix_membership;
CREATE DATABASE gix_facility;
CREATE DATABASE gix_booking;
CREATE DATABASE gix_payment;
CREATE DATABASE gix_notification;

CREATE ROLE gix_membership WITH LOGIN PASSWORD '${MEMBERSHIP_DB_PASSWORD}';
CREATE ROLE gix_facility WITH LOGIN PASSWORD '${FACILITY_DB_PASSWORD}';
CREATE ROLE gix_booking WITH LOGIN PASSWORD '${BOOKING_DB_PASSWORD}';
CREATE ROLE gix_payment WITH LOGIN PASSWORD '${PAYMENT_DB_PASSWORD}';
CREATE ROLE gix_notification WITH LOGIN PASSWORD '${NOTIFICATION_DB_PASSWORD}';

GRANT CONNECT ON DATABASE gix_membership TO gix_membership;
GRANT CONNECT ON DATABASE gix_facility TO gix_facility;
GRANT CONNECT ON DATABASE gix_booking TO gix_booking;
GRANT CONNECT ON DATABASE gix_payment TO gix_payment;
GRANT CONNECT ON DATABASE gix_notification TO gix_notification;

-- Ownership for Flyway
ALTER DATABASE gix_membership OWNER TO gix_membership;
ALTER DATABASE gix_facility OWNER TO gix_facility;
ALTER DATABASE gix_booking OWNER TO gix_booking;
ALTER DATABASE gix_payment OWNER TO gix_payment;
ALTER DATABASE gix_notification OWNER TO gix_notification;
```

**Note**: `init-sql` scripts run **only when the data volume is empty**. To reset:
```bash
docker compose down -v  # removes volumes
docker compose up -d    # re-runs init scripts
```

### Verification Commands

```bash
docker compose -f infrastructure/docker/compose.yml config     # validates syntax
docker compose -f infrastructure/docker/compose.yml up -d      # starts stack
docker compose -f infrastructure/docker/compose.yml ps         # shows health
docker compose -f infrastructure/docker/compose.yml logs -f    # tails logs
```

## Consequences

### Positive
- One command (`docker compose up -d`) starts full local infra
- No cloud accounts, no costs, works offline
- Volumes persist data across restarts
- Health checks enable `depends_on: condition: service_healthy`

### Negative
- Requires Docker Desktop with WSL 2 integration (not available in bare WSL)
- Schema Registry adds startup time and memory (~500MB)
- Single Kafka broker: no replication (local only)
- Volume reset loses all data (intentional for clean slate)

## Verification

- `docker compose config` exits 0
- All services report `healthy` in `docker compose ps`
- `psql -h localhost -U gix_membership -d gix_membership -c 'SELECT 1'`
- `kafka-topics --bootstrap-server localhost:9092 --list`

## References

- [Docker Compose Spec](https://docs.docker.com/compose/compose-file/)
- [Confluent Platform Images](https://docs.confluent.io/platform/current/installation/docker/image-reference.html)
- ADR 0003 (Database per Service), ADR 0005 (Kafka), ADR 0010 (AWS Deferred)