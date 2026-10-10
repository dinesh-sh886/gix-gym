/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "outbox_event",
        indexes = {
                @Index(name = "idx_outbox_unpublished", columnList = "published_at"),
                @Index(name = "idx_outbox_aggregate", columnList = "aggregate_id"),
                @Index(name = "idx_outbox_org_id", columnList = "organization_id")
        })
public class OutboxEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 200)
    private String eventType;

    @Column(name = "schema_version", nullable = false, length = 20)
    private String schemaVersion;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OutboxEvent() {
        // JPA required
    }

    private OutboxEvent(UUID eventId, String eventType, String schemaVersion,
                        String aggregateType, String aggregateId, UUID organizationId,
                        Instant occurredAt, Map<String, Object> payload, Map<String, Object> metadata) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.schemaVersion = schemaVersion;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.organizationId = organizationId;
        this.occurredAt = occurredAt;
        this.payload = payload;
        this.metadata = metadata;
        this.createdAt = Instant.now();
    }

    public static OutboxEvent create(String eventType, String schemaVersion,
                                      String aggregateType, String aggregateId,
                                      UUID organizationId, Instant occurredAt,
                                      Map<String, Object> payload, Map<String, Object> metadata) {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), eventType, schemaVersion,
                aggregateType, aggregateId, organizationId, occurredAt, payload, metadata);
        return event;
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public boolean isPublished() {
        return publishedAt != null;
    }

    // Getters
    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OutboxEvent)) return false;
        OutboxEvent that = (OutboxEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "OutboxEvent{" +
                "eventId=" + eventId +
                ", eventType='" + eventType + '\'' +
                ", aggregateType='" + aggregateType + '\'' +
                ", aggregateId='" + aggregateId + '\'' +
                ", organizationId=" + organizationId +
                ", published=" + isPublished() +
                '}';
    }
}