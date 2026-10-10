/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gix.gym.membership.domain.OutboxEvent;
import com.gix.gym.membership.persistence.OutboxEventRepository;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);
    private static final int BATCH_SIZE = 100;
    private static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final SchemaRegistryClient schemaRegistryClient;

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository,
                                KafkaTemplate<String, byte[]> kafkaTemplate,
                                ObjectMapper objectMapper,
                                SchemaRegistryClient schemaRegistryClient) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.schemaRegistryClient = schemaRegistryClient;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findUnpublishedEvents();
        if (events.isEmpty()) {
            return;
        }

        log.debug("Found {} unpublished outbox events", events.size());

        for (OutboxEvent event : events) {
            try {
                publishEvent(event);
                event.markPublished(Instant.now());
                outboxEventRepository.save(event);
                log.info("Published outbox event {} of type {} for aggregate {}",
                        event.getEventId(), event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getEventId(), e.getMessage(), e);
                // Don't rethrow - let the next scheduled run retry
            }
        }
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    protected void publishEvent(OutboxEvent event) throws Exception {
        String topic = getTopicForEventType(event.getEventType());
        String key = event.getAggregateId();
        byte[] value = serializeEvent(event);

        CompletableFuture<SendResult<String, byte[]>> future = kafkaTemplate.send(topic, key, value);
        future.get(); // Wait for broker acknowledgement
    }

    private String getTopicForEventType(String eventType) {
        // Map event types to Kafka topics
        return switch (eventType) {
            case "membership.created" -> "membership.membership.created";
            case "membership.frozen" -> "membership.membership.frozen";
            case "membership.cancelled" -> "membership.membership.cancelled";
            default -> "membership.unknown";
        };
    }

    private byte[] serializeEvent(OutboxEvent event) throws Exception {
        // Get schema from Schema Registry
        Schema schema = schemaRegistryClient.getSchema(event.getEventType(), event.getSchemaVersion());
        
        // Convert payload to Avro GenericRecord
        GenericRecord record = new GenericData.Record(schema);
        record.put("eventId", event.getEventId().toString());
        record.put("eventType", event.getEventType());
        record.put("schemaVersion", event.getSchemaVersion());
        record.put("aggregateType", event.getAggregateType());
        record.put("aggregateId", event.getAggregateId());
        record.put("organizationId", event.getOrganizationId().toString());
        record.put("occurredAt", event.getOccurredAt().toString());
        
        // Add event-specific fields from payload
        if (event.getPayload() != null) {
            for (Map.Entry<String, Object> entry : event.getPayload().entrySet()) {
                if (schema.getField(entry.getKey()) != null) {
                    record.put(entry.getKey(), entry.getValue());
                }
            }
        }
        
        // Add metadata fields
        if (event.getMetadata() != null) {
            if (event.getMetadata().containsKey("correlationId")) {
                record.put("correlationId", event.getMetadata().get("correlationId"));
            }
            if (event.getMetadata().containsKey("causationId")) {
                record.put("causationId", event.getMetadata().get("causationId"));
            }
        }

        // Serialize to binary Avro
        return AvroSerializer.serialize(record, schema);
    }
}