/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.application;

import com.gix.gym.membership.api.MembershipResponse;
import com.gix.gym.membership.domain.MembershipStatus;
import com.gix.gym.membership.domain.OutboxEvent;
import com.gix.gym.membership.persistence.MembershipRepository;
import com.gix.gym.membership.persistence.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@Transactional
class MembershipServiceIntegrationTest {

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
    private MembershipService membershipService;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private static final UUID ORG_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ORG_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID BRANCH_ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BRANCH_ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID MEMBER_ID_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MEMBER_ID_2 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Instant START_DATE = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant END_DATE = Instant.parse("2026-12-31T23:59:59Z");
    private static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z");

    @Test
    @DisplayName("Should create membership and outbox event atomically")
    void shouldCreateMembershipAndOutboxEventAtomically() {
        MembershipResponse response = membershipService.createMembership(
                ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium Monthly", START_DATE, END_DATE);

        assertThat(response.getMembershipId()).isNotNull();
        assertThat(response.getOrganizationId()).isEqualTo(ORG_ID_1);
        assertThat(response.getBranchId()).isEqualTo(BRANCH_ID_1);
        assertThat(response.getMemberId()).isEqualTo(MEMBER_ID_1);
        assertThat(response.getPlanName()).isEqualTo("Premium Monthly");
        assertThat(response.getStatus()).isEqualTo(MembershipStatus.ACTIVE);

        // Verify outbox event was created
        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(response.getMembershipId().toString());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo("membership.created");
        assertThat(events.get(0).getOrganizationId()).isEqualTo(ORG_ID_1);
        assertThat(events.get(0).isPublished()).isFalse();
    }

    @Test
    @DisplayName("Should reject duplicate active membership for same member in organization")
    void shouldRejectDuplicateActiveMembership() {
        membershipService.createMembership(ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);

        assertThatThrownBy(() -> membershipService.createMembership(
                ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Basic", START_DATE, END_DATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has an active or frozen membership");
    }

    @Test
    @DisplayName("Should allow membership for same member in different organization")
    void shouldAllowMembershipInDifferentOrganization() {
        membershipService.createMembership(ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);

        MembershipResponse response = membershipService.createMembership(
                ORG_ID_2, BRANCH_ID_2, MEMBER_ID_1, "Basic", START_DATE, END_DATE);

        assertThat(response.getOrganizationId()).isEqualTo(ORG_ID_2);
        assertThat(response.getMemberId()).isEqualTo(MEMBER_ID_1);
    }

    @Test
    @DisplayName("Should get membership by ID within organization")
    void shouldGetMembershipByIdWithinOrganization() {
        MembershipResponse created = membershipService.createMembership(
                ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);

        MembershipResponse found = membershipService.getMembership(ORG_ID_1, created.getMembershipId());

        assertThat(found.getMembershipId()).isEqualTo(created.getMembershipId());
        assertThat(found.getPlanName()).isEqualTo("Premium");
    }

    @Test
    @DisplayName("Should throw when getting membership from different organization")
    void shouldThrowWhenGettingMembershipFromDifferentOrganization() {
        MembershipResponse created = membershipService.createMembership(
                ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);

        assertThatThrownBy(() -> membershipService.getMembership(ORG_ID_2, created.getMembershipId()))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("Membership not found");
    }

    @Test
    @DisplayName("Should list memberships with pagination")
    void shouldListMembershipsWithPagination() {
        membershipService.createMembership(ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);
        membershipService.createMembership(ORG_ID_1, BRANCH_ID_1, MEMBER_ID_2, "Basic", START_DATE, END_DATE);
        membershipService.createMembership(ORG_ID_1, BRANCH_ID_2, MEMBER_ID_1, "VIP", START_DATE, END_DATE);

        var page = membershipService.listMemberships(ORG_ID_1, BRANCH_ID_1,
                org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(MembershipResponse::getBranchId).containsOnly(BRANCH_ID_1);
    }

    @Test
    @DisplayName("Should freeze membership and create outbox event")
    void shouldFreezeMembershipAndCreateOutboxEvent() {
        MembershipResponse created = membershipService.createMembership(
                ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);

        MembershipResponse frozen = membershipService.freezeMembership(ORG_ID_1, created.getMembershipId(), NOW);

        assertThat(frozen.getStatus()).isEqualTo(MembershipStatus.FROZEN);
        assertThat(frozen.getFrozenAt()).isEqualTo(NOW);

        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(created.getMembershipId().toString());
        assertThat(events).hasSize(2);
        assertThat(events).extracting(OutboxEvent::getEventType)
                .containsExactly("membership.created", "membership.frozen");
    }

    @Test
    @DisplayName("Should cancel membership and create outbox event")
    void shouldCancelMembershipAndCreateOutboxEvent() {
        MembershipResponse created = membershipService.createMembership(
                ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);

        MembershipResponse cancelled = membershipService.cancelMembership(ORG_ID_1, created.getMembershipId(), NOW);

        assertThat(cancelled.getStatus()).isEqualTo(MembershipStatus.CANCELLED);
        assertThat(cancelled.getCancelledAt()).isEqualTo(NOW);

        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(created.getMembershipId().toString());
        assertThat(events).hasSize(2);
        assertThat(events).extracting(OutboxEvent::getEventType)
                .containsExactly("membership.created", "membership.cancelled");
    }

    @Test
    @DisplayName("Should reject invalid state transitions")
    void shouldRejectInvalidStateTransitions() {
        MembershipResponse created = membershipService.createMembership(
                ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);

        // Freeze then cancel
        membershipService.freezeMembership(ORG_ID_1, created.getMembershipId(), NOW);
        MembershipResponse cancelled = membershipService.cancelMembership(ORG_ID_1, created.getMembershipId(), NOW.plusSeconds(3600));
        assertThat(cancelled.getStatus()).isEqualTo(MembershipStatus.CANCELLED);

        // Try to freeze cancelled membership
        assertThatThrownBy(() -> membershipService.freezeMembership(
                ORG_ID_1, created.getMembershipId(), NOW.plusSeconds(7200)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot freeze membership from status: CANCELLED");
    }

    @Test
    @DisplayName("Should enforce tenant isolation for freeze operation")
    void shouldEnforceTenantIsolationForFreeze() {
        MembershipResponse created = membershipService.createMembership(
                ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);

        assertThatThrownBy(() -> membershipService.freezeMembership(
                ORG_ID_2, created.getMembershipId(), NOW))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Should enforce tenant isolation for cancel operation")
    void shouldEnforceTenantIsolationForCancel() {
        MembershipResponse created = membershipService.createMembership(
                ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);

        assertThatThrownBy(() -> membershipService.cancelMembership(
                ORG_ID_2, created.getMembershipId(), NOW))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }
}