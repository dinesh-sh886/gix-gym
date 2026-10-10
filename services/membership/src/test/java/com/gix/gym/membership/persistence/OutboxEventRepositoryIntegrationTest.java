/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.persistence;

import com.gix.gym.membership.domain.OutboxEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
class OutboxEventRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("gix_membership")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private static final UUID ORG_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ORG_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String AGGREGATE_ID = UUID.randomUUID().toString();
    private static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z");

    @Test
    @DisplayName("Should save and find unpublished events")
    void shouldSaveAndFindUnpublishedEvents() {
        Map<String, Object> payload = Map.of("test", "data");
        OutboxEvent event1 = OutboxEvent.create("membership.created", "1.0", "Membership",
                AGGREGATE_ID, ORG_ID_1, NOW, payload, null);
        OutboxEvent event2 = OutboxEvent.create("membership.frozen", "1.0", "Membership",
                AGGREGATE_ID, ORG_ID_1, NOW, payload, null);
        event2.markPublished(Instant.now());
        outboxEventRepository.saveAll(List.of(event1, event2));

        List<OutboxEvent> unpublished = outboxEventRepository.findUnpublishedEvents();

        assertThat(unpublished).hasSize(1);
        assertThat(unpublished.get(0).getEventType()).isEqualTo("membership.created");
        assertThat(unpublished.get(0).isPublished()).isFalse();
    }

    @Test
    @DisplayName("Should find unpublished events by organization")
    void shouldFindUnpublishedEventsByOrganization() {
        Map<String, Object> payload = Map.of("test", "data");
        OutboxEvent event1 = OutboxEvent.create("membership.created", "1.0", "Membership",
                AGGREGATE_ID, ORG_ID_1, NOW, payload, null);
        OutboxEvent event2 = OutboxEvent.create("membership.created", "1.0", "Membership",
                AGGREGATE_ID, ORG_ID_2, NOW, payload, null);
        outboxEventRepository.saveAll(List.of(event1, event2));

        List<OutboxEvent> org1Events = outboxEventRepository.findUnpublishedEventsByOrganization(ORG_ID_1);
        List<OutboxEvent> org2Events = outboxEventRepository.findUnpublishedEventsByOrganization(ORG_ID_2);

        assertThat(org1Events).hasSize(1);
        assertThat(org1Events.get(0).getOrganizationId()).isEqualTo(ORG_ID_1);
        assertThat(org2Events).hasSize(1);
        assertThat(org2Events.get(0).getOrganizationId()).isEqualTo(ORG_ID_2);
    }

    @Test
    @DisplayName("Should find unpublished event by ID")
    void shouldFindUnpublishedEventById() {
        Map<String, Object> payload = Map.of("test", "data");
        OutboxEvent event = OutboxEvent.create("membership.created", "1.0", "Membership",
                AGGREGATE_ID, ORG_ID_1, NOW, payload, null);
        outboxEventRepository.save(event);

        Optional<OutboxEvent> found = outboxEventRepository.findByEventIdAndPublishedAtIsNull(event.getEventId());

        assertThat(found).isPresent();
        assertThat(found.get().getEventId()).isEqualTo(event.getEventId());
    }

    @Test
    @DisplayName("Should not find published event by ID")
    void shouldNotFindPublishedEventById() {
        Map<String, Object> payload = Map.of("test", "data");
        OutboxEvent event = OutboxEvent.create("membership.created", "1.0", "Membership",
                AGGREGATE_ID, ORG_ID_1, NOW, payload, null);
        event.markPublished(Instant.now());
        outboxEventRepository.save(event);

        Optional<OutboxEvent> found = outboxEventRepository.findByEventIdAndPublishedAtIsNull(event.getEventId());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should count unpublished events")
    void shouldCountUnpublishedEvents() {
        Map<String, Object> payload = Map.of("test", "data");
        OutboxEvent event1 = OutboxEvent.create("membership.created", "1.0", "Membership",
                AGGREGATE_ID, ORG_ID_1, NOW, payload, null);
        OutboxEvent event2 = OutboxEvent.create("membership.frozen", "1.0", "Membership",
                AGGREGATE_ID, ORG_ID_1, NOW, payload, null);
        event2.markPublished(Instant.now());
        outboxEventRepository.saveAll(List.of(event1, event2));

        long count = outboxEventRepository.countUnpublishedEvents();

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find events by aggregate ID")
    void shouldFindEventsByAggregateId() {
        Map<String, Object> payload = Map.of("test", "data");
        OutboxEvent event1 = OutboxEvent.create("membership.created", "1.0", "Membership",
                AGGREGATE_ID, ORG_ID_1, NOW, payload, null);
        OutboxEvent event2 = OutboxEvent.create("membership.frozen", "1.0", "Membership",
                AGGREGATE_ID, ORG_ID_1, NOW.plusSeconds(3600), payload, null);
        String otherAggregateId = UUID.randomUUID().toString();
        OutboxEvent event3 = OutboxEvent.create("membership.created", "1.0", "Membership",
                otherAggregateId, ORG_ID_1, NOW, payload, null);
        outboxEventRepository.saveAll(List.of(event1, event2, event3));

        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(AGGREGATE_ID);

        assertThat(events).hasSize(2);
        assertThat(events).extracting(OutboxEvent::getEventType).containsExactly("membership.created", "membership.frozen");
    }
}