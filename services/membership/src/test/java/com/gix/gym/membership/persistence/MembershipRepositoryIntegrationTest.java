/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.persistence;

import com.gix.gym.membership.domain.Membership;
import com.gix.gym.membership.domain.MembershipStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
class MembershipRepositoryIntegrationTest {

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
    private MembershipRepository membershipRepository;

    private static final UUID ORG_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ORG_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID BRANCH_ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BRANCH_ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID MEMBER_ID_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MEMBER_ID_2 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Instant START_DATE = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant END_DATE = Instant.parse("2026-12-31T23:59:59Z");

    @Test
    @DisplayName("Should save and find membership by ID and organization")
    void shouldSaveAndFindByIdAndOrganization() {
        Membership membership = Membership.create(ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);
        membershipRepository.save(membership);

        Optional<Membership> found = membershipRepository.findByMembershipIdAndOrganizationId(
                membership.getMembershipId(), ORG_ID_1);

        assertThat(found).isPresent();
        assertThat(found.get().getMembershipId()).isEqualTo(membership.getMembershipId());
        assertThat(found.get().getOrganizationId()).isEqualTo(ORG_ID_1);
        assertThat(found.get().getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should not find membership from different organization")
    void shouldNotFindMembershipFromDifferentOrganization() {
        Membership membership = Membership.create(ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);
        membershipRepository.save(membership);

        Optional<Membership> found = membershipRepository.findByMembershipIdAndOrganizationId(
                membership.getMembershipId(), ORG_ID_2);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find memberships by organization and branch with pagination")
    void shouldFindByOrganizationAndBranchWithPagination() {
        Membership m1 = Membership.create(ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);
        Membership m2 = Membership.create(ORG_ID_1, BRANCH_ID_1, MEMBER_ID_2, "Basic", START_DATE, END_DATE);
        Membership m3 = Membership.create(ORG_ID_1, BRANCH_ID_2, MEMBER_ID_1, "Premium", START_DATE, END_DATE);
        membershipRepository.saveAll(List.of(m1, m2, m3));

        Pageable pageable = PageRequest.of(0, 10);
        var page = membershipRepository.findByOrganizationIdAndBranchId(ORG_ID_1, BRANCH_ID_1, pageable);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Membership::getBranchId).containsOnly(BRANCH_ID_1);
    }

    @Test
    @DisplayName("Should find memberships by organization and member")
    void shouldFindByOrganizationAndMember() {
        Membership m1 = Membership.create(ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);
        Membership m2 = Membership.create(ORG_ID_1, BRANCH_ID_2, MEMBER_ID_1, "Basic", START_DATE, END_DATE);
        Membership m3 = Membership.create(ORG_ID_2, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);
        membershipRepository.saveAll(List.of(m1, m2, m3));

        List<Membership> found = membershipRepository.findByOrganizationIdAndMemberId(ORG_ID_1, MEMBER_ID_1);

        assertThat(found).hasSize(2);
        assertThat(found).extracting(Membership::getOrganizationId).containsOnly(ORG_ID_1);
        assertThat(found).extracting(Membership::getMemberId).containsOnly(MEMBER_ID_1);
    }

    @Test
    @DisplayName("Should find memberships by organization and status")
    void shouldFindByOrganizationAndStatus() {
        Membership m1 = Membership.create(ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);
        Membership m2 = Membership.create(ORG_ID_1, BRANCH_ID_1, MEMBER_ID_2, "Basic", START_DATE, END_DATE);
        m2.freeze(Instant.now());
        membershipRepository.saveAll(List.of(m1, m2));

        List<Membership> active = membershipRepository.findByOrganizationIdAndStatus(ORG_ID_1, MembershipStatus.ACTIVE);
        List<Membership> frozen = membershipRepository.findByOrganizationIdAndStatus(ORG_ID_1, MembershipStatus.FROZEN);

        assertThat(active).hasSize(1);
        assertThat(frozen).hasSize(1);
    }

    @Test
    @DisplayName("Should check if member has active membership in organization")
    void shouldCheckMemberHasActiveMembership() {
        Membership m1 = Membership.create(ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);
        membershipRepository.save(m1);

        boolean hasActive = membershipRepository.existsByOrganizationIdAndMemberIdAndStatusIn(
                ORG_ID_1, MEMBER_ID_1, List.of(MembershipStatus.ACTIVE, MembershipStatus.FROZEN));

        assertThat(hasActive).isTrue();
    }

    @Test
    @DisplayName("Should return false when member has no active membership")
    void shouldReturnFalseWhenNoActiveMembership() {
        Membership m1 = Membership.create(ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);
        m1.cancel(Instant.now());
        membershipRepository.save(m1);

        boolean hasActive = membershipRepository.existsByOrganizationIdAndMemberIdAndStatusIn(
                ORG_ID_1, MEMBER_ID_1, List.of(MembershipStatus.ACTIVE, MembershipStatus.FROZEN));

        assertThat(hasActive).isFalse();
    }

    @Test
    @DisplayName("Should count memberships by organization")
    void shouldCountMembershipsByOrganization() {
        Membership m1 = Membership.create(ORG_ID_1, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);
        Membership m2 = Membership.create(ORG_ID_1, BRANCH_ID_2, MEMBER_ID_2, "Basic", START_DATE, END_DATE);
        Membership m3 = Membership.create(ORG_ID_2, BRANCH_ID_1, MEMBER_ID_1, "Premium", START_DATE, END_DATE);
        membershipRepository.saveAll(List.of(m1, m2, m3));

        long countOrg1 = membershipRepository.countByOrganizationId(ORG_ID_1);
        long countOrg2 = membershipRepository.countByOrganizationId(ORG_ID_2);

        assertThat(countOrg1).isEqualTo(2);
        assertThat(countOrg2).isEqualTo(1);
    }
}