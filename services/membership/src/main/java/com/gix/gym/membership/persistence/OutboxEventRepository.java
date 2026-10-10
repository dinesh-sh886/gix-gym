/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.persistence;

import com.gix.gym.membership.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("SELECT e FROM OutboxEvent e WHERE e.publishedAt IS NULL ORDER BY e.createdAt ASC")
    List<OutboxEvent> findUnpublishedEvents();

    @Query("SELECT e FROM OutboxEvent e WHERE e.publishedAt IS NULL AND e.organizationId = :orgId ORDER BY e.createdAt ASC")
    List<OutboxEvent> findUnpublishedEventsByOrganization(@Param("orgId") UUID organizationId);

    Optional<OutboxEvent> findByEventIdAndPublishedAtIsNull(UUID eventId);

    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.publishedAt IS NULL")
    long countUnpublishedEvents();

    @Query("SELECT e FROM OutboxEvent e WHERE e.aggregateId = :aggregateId ORDER BY e.occurredAt ASC")
    List<OutboxEvent> findByAggregateId(@Param("aggregateId") String aggregateId);
}