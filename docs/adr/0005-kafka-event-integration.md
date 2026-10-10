# ADR 0005: Kafka Event Integration, Transactional Outbox, and Idempotency

**Status:** proposed
**Owner:** dinesh-sh886
**Date:** 2026-10-10

## Context

Microservices need reliable async communication. Direct HTTP calls create temporal coupling and cascade failures. Kafka provides durable, ordered, replayable event streams. However, publishing events atomically with database updates requires the **transactional outbox pattern**.

## Decision

### Event-Driven Architecture

- **Kafka** as the event backbone (local: KRaft mode, no ZooKeeper)
- **Event types**: Domain events (e.g., `MemberRegistered`, `BookingConfirmed`), Integration events (cross-service)
- **Topic naming**: `<service>.<entity>.<action>` (e.g., `membership.member.registered`)
- **Partitioning**: By aggregate ID (e.g., `memberId`) for ordering guarantees per entity
- **Serialization**: Avro with Schema Registry (local: Confluent Schema Registry)
- **Contracts**: AsyncAPI specs in `contracts/kafka-events/`

### Transactional Outbox Pattern

Every service that publishes events implements:

1. **Outbox table** (`outbox_event`) in the service's database:
   ```sql
   CREATE TABLE outbox_event (
     id UUID PRIMARY KEY,
     aggregate_type VARCHAR(100) NOT NULL,
     aggregate_id VARCHAR(100) NOT NULL,
     event_type VARCHAR(200) NOT NULL,
     payload JSONB NOT NULL,
     metadata JSONB,
     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
     published_at TIMESTAMPTZ
   );
   CREATE INDEX idx_outbox_unpublished ON outbox_event (published_at) WHERE published_at IS NULL;
   ```

2. **Same transaction** writes domain changes + outbox row
3. **Relay** (separate thread/process) polls unpublished events → publishes to Kafka → marks `published_at`
   - Local: Spring Kafka `KafkaTemplate` + `@Scheduled` poller or Debezium CDC (future)
   - At-least-once delivery guaranteed

### Idempotent Consumers

- Consumers **must** handle duplicate events (at-least-once)
- **Idempotency key**: `event_id` (UUID from outbox) stored in `processed_event` table
- Consumer logic:
  ```java
  if (processedEventRepo.existsById(event.getEventId())) return; // already handled
  try { handle(event); processedEventRepo.save(new ProcessedEvent(event.getEventId())); }
  catch (Exception e) { throw e; } // let retry happen
  ```
- `processed_event` table has TTL-based cleanup (e.g., 30 days)

### Local Kafka Setup

- Single broker (KRaft) on port 9092
- Schema Registry on port 8081 (optional, can defer if blocking)
- Topics auto-created with 3 partitions, replication factor 1 (local only)

## Consequences

### Positive
- **Atomicity**: DB update + event publish never diverge
- **Reliability**: Events persisted before publish; relay retries on failure
- **Replayability**: Consumers can reprocess from offset
- **Decoupling**: Services don't need to know each other's APIs

### Negative
- **Eventual consistency**: Consumers see state after delay (ms to seconds)
- **Complexity**: Outbox table, relay, idempotency storage per service
- **Schema evolution**: Requires careful Avro compatibility rules (BACKWARD)
- **Operational**: Kafka adds infrastructure (mitigated by single local broker)

## Verification

- Integration test: DB transaction rolls back → no event in Kafka
- Integration test: Relay publishes → consumer processes → idempotent re-delivery ignored
- `kafka-topics --list` shows service topics
- Schema Registry `GET /subjects` returns registered schemas (when enabled)

## References

- [Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)
- [Confluent Schema Registry](https://docs.confluent.io/platform/current/schema-registry/index.html)
- ADR 0003 (Database per Service), ADR 0006 (REST & OpenAPI)