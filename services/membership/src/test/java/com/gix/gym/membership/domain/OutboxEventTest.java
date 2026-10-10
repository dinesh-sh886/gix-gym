/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventTest {

    private static final UUID ORG_ID = UUID.randomUUID();
    private static final String EVENT_TYPE = "membership.created";
    private static final String SCHEMA_VERSION = "1.0";
    private static final String AGGREGATE_TYPE = "Membership";
    private static final String AGGREGATE_ID = UUID.randomUUID().toString();
    private static final Instant OCCURRED_AT = Instant.parse("2026-06-15T12:00:00Z");
    private static final Map<String, Object> PAYLOAD = Map.of("membershipId", UUID.randomUUID().toString());
    private static final Map<String, Object> METADATA = Map.of("correlationId", "test-correlation");

    @Test
    @DisplayName("Should create outbox event with all fields")
    void shouldCreateOutboxEvent() {
        OutboxEvent event = OutboxEvent.create(EVENT_TYPE, SCHEMA_VERSION, AGGREGATE_TYPE,
                AGGREGATE_ID, ORG_ID, OCCURRED_AT, PAYLOAD, METADATA);

        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventType()).isEqualTo(EVENT_TYPE);
        assertThat(event.getSchemaVersion()).isEqualTo(SCHEMA_VERSION);
        assertThat(event.getAggregateType()).isEqualTo(AGGREGATE_TYPE);
        assertThat(event.getAggregateId()).isEqualTo(AGGREGATE_ID);
        assertThat(event.getOrganizationId()).isEqualTo(ORG_ID);
        assertThat(event.getOccurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(event.getPayload()).isEqualTo(PAYLOAD);
        assertThat(event.getMetadata()).isEqualTo(METADATA);
        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.isPublished()).isFalse();
    }

    @Test
    @DisplayName("Should create outbox event without metadata")
    void shouldCreateOutboxEventWithoutMetadata() {
        OutboxEvent event = OutboxEvent.create(EVENT_TYPE, SCHEMA_VERSION, AGGREGATE_TYPE,
                AGGREGATE_ID, ORG_ID, OCCURRED_AT, PAYLOAD, null);

        assertThat(event.getMetadata()).isNull();
    }

    @Test
    @DisplayName("Should mark event as published")
    void shouldMarkEventAsPublished() {
        OutboxEvent event = OutboxEvent.create(EVENT_TYPE, SCHEMA_VERSION, AGGREGATE_TYPE,
                AGGREGATE_ID, ORG_ID, OCCURRED_AT, Map.of("test", "data"), Map.of("key", "value"));

        Instant publishedAt = Instant.parse("2026-06-15T12:00:01Z");
        event.markPublished(publishedAt);

        assertThat(event.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(event.isPublished()).isTrue();
    }

    @Test
    @DisplayName("Should implement equals and hashCode based on eventId")
    void shouldImplementEqualsAndHashCode() {
        Map<String, Object> payload = Map.of("test", "data");
        Map<String, Object> metadata = Map.of("key", "value");
        OutboxEvent event1 = OutboxEvent.create(EVENT_TYPE, SCHEMA_VERSION, AGGREGATE_TYPE,
                AGGREGATE_ID, ORG_ID, OCCURRED_AT, payload, metadata);
        OutboxEvent event2 = OutboxEvent.create(EVENT_TYPE, SCHEMA_VERSION, AGGREGATE_TYPE,
                AGGREGATE_ID, ORG_ID, OCCURRED_AT, payload, metadata);

        assertThat(event1).isNotEqualTo(event2);
        assertThat(event1).isEqualTo(event1);
        assertThat(event1.hashCode()).isEqualTo(event1.hashCode());
    }
}