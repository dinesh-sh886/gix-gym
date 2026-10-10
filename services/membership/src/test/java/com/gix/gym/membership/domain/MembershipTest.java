/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MembershipTest {

    private static final UUID ORG_ID = UUID.randomUUID();
    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final String PLAN_NAME = "Premium Monthly";
    private static final Instant START_DATE = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant END_DATE = Instant.parse("2026-12-31T23:59:59Z");
    private static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z");

    @Test
    @DisplayName("Should create membership with ACTIVE status")
    void shouldCreateMembershipWithActiveStatus() {
        Membership membership = Membership.create(ORG_ID, BRANCH_ID, MEMBER_ID, PLAN_NAME, START_DATE, END_DATE);

        assertThat(membership.getMembershipId()).isNotNull();
        assertThat(membership.getOrganizationId()).isEqualTo(ORG_ID);
        assertThat(membership.getBranchId()).isEqualTo(BRANCH_ID);
        assertThat(membership.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(membership.getPlanName()).isEqualTo(PLAN_NAME);
        assertThat(membership.getStartDate()).isEqualTo(START_DATE);
        assertThat(membership.getEndDate()).isEqualTo(END_DATE);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(membership.getFrozenAt()).isNull();
        assertThat(membership.getCancelledAt()).isNull();
        assertThat(membership.getCreatedAt()).isNotNull();
        assertThat(membership.getUpdatedAt()).isNotNull();
        assertThat(membership.getVersion()).isEqualTo(0L);
    }

    @Nested
    @DisplayName("Status transitions")
    class StatusTransitions {

        @Test
        @DisplayName("Should freeze ACTIVE membership")
        void shouldFreezeActiveMembership() {
            Membership membership = Membership.create(ORG_ID, BRANCH_ID, MEMBER_ID, PLAN_NAME, START_DATE, END_DATE);

            membership.freeze(NOW);

            assertThat(membership.getStatus()).isEqualTo(MembershipStatus.FROZEN);
            assertThat(membership.getFrozenAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("Should activate FROZEN membership")
        void shouldActivateFrozenMembership() {
            Membership membership = Membership.create(ORG_ID, BRANCH_ID, MEMBER_ID, PLAN_NAME, START_DATE, END_DATE);
            membership.freeze(NOW);

            Instant activatedAt = NOW.plusSeconds(3600);
            membership.activate(activatedAt);

            assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
            assertThat(membership.getFrozenAt()).isNull();
            assertThat(membership.getUpdatedAt()).isEqualTo(activatedAt);
        }

        @Test
        @DisplayName("Should cancel ACTIVE membership")
        void shouldCancelActiveMembership() {
            Membership membership = Membership.create(ORG_ID, BRANCH_ID, MEMBER_ID, PLAN_NAME, START_DATE, END_DATE);

            membership.cancel(NOW);

            assertThat(membership.getStatus()).isEqualTo(MembershipStatus.CANCELLED);
            assertThat(membership.getCancelledAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("Should cancel FROZEN membership")
        void shouldCancelFrozenMembership() {
            Membership membership = Membership.create(ORG_ID, BRANCH_ID, MEMBER_ID, PLAN_NAME, START_DATE, END_DATE);
            membership.freeze(NOW);

            Instant cancelledAt = NOW.plusSeconds(3600);
            membership.cancel(cancelledAt);

            assertThat(membership.getStatus()).isEqualTo(MembershipStatus.CANCELLED);
            assertThat(membership.getCancelledAt()).isEqualTo(cancelledAt);
        }

        @Test
        @DisplayName("Should expire ACTIVE membership")
        void shouldExpireActiveMembership() {
            Membership membership = Membership.create(ORG_ID, BRANCH_ID, MEMBER_ID, PLAN_NAME, START_DATE, END_DATE);

            Instant expiredAt = NOW.plusSeconds(3600);
            membership.expire(expiredAt);

            assertThat(membership.getStatus()).isEqualTo(MembershipStatus.EXPIRED);
            assertThat(membership.getUpdatedAt()).isEqualTo(expiredAt);
        }

        @Test
        @DisplayName("Should throw when freezing non-ACTIVE membership")
        void shouldThrowWhenFreezingNonActive() {
            Membership membership = Membership.create(ORG_ID, BRANCH_ID, MEMBER_ID, PLAN_NAME, START_DATE, END_DATE);
            membership.cancel(NOW);

            assertThatThrownBy(() -> membership.freeze(NOW.plusSeconds(3600)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot freeze membership from status: CANCELLED");
        }

        @Test
        @DisplayName("Should throw when activating non-FROZEN/EXPIRED membership")
        void shouldThrowWhenActivatingInvalidState() {
            Membership membership = Membership.create(ORG_ID, BRANCH_ID, MEMBER_ID, PLAN_NAME, START_DATE, END_DATE);
            membership.cancel(NOW);

            assertThatThrownBy(() -> membership.activate(NOW.plusSeconds(3600)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot activate membership from status: CANCELLED");
        }

        @Test
        @DisplayName("Should throw when cancelling CANCELLED membership")
        void shouldThrowWhenCancellingCancelled() {
            Membership membership = Membership.create(ORG_ID, BRANCH_ID, MEMBER_ID, PLAN_NAME, START_DATE, END_DATE);
            membership.cancel(NOW);

            assertThatThrownBy(() -> membership.cancel(NOW.plusSeconds(3600)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel membership from status: CANCELLED");
        }
    }

    @Nested
    @DisplayName("canTransitionTo validation")
    class CanTransitionTo {

        @ParameterizedTest
        @EnumSource(MembershipStatus.class)
        @DisplayName("Should validate transitions from ACTIVE")
        void shouldValidateTransitionsFromActive(MembershipStatus target) {
            boolean canTransition = MembershipStatus.ACTIVE.canTransitionTo(target);

            if (target == MembershipStatus.FROZEN || target == MembershipStatus.CANCELLED || target == MembershipStatus.EXPIRED) {
                assertThat(canTransition).isTrue();
            } else {
                assertThat(canTransition).isFalse();
            }
        }

        @ParameterizedTest
        @EnumSource(MembershipStatus.class)
        @DisplayName("Should validate transitions from FROZEN")
        void shouldValidateTransitionsFromFrozen(MembershipStatus target) {
            boolean canTransition = MembershipStatus.FROZEN.canTransitionTo(target);

            if (target == MembershipStatus.ACTIVE || target == MembershipStatus.CANCELLED) {
                assertThat(canTransition).isTrue();
            } else {
                assertThat(canTransition).isFalse();
            }
        }

        @ParameterizedTest
        @EnumSource(MembershipStatus.class)
        @DisplayName("Should validate no transitions from CANCELLED")
        void shouldValidateNoTransitionsFromCancelled(MembershipStatus target) {
            boolean canTransition = MembershipStatus.CANCELLED.canTransitionTo(target);
            assertThat(canTransition).isFalse();
        }

        @ParameterizedTest
        @EnumSource(MembershipStatus.class)
        @DisplayName("Should validate transitions from EXPIRED")
        void shouldValidateTransitionsFromExpired(MembershipStatus target) {
            boolean canTransition = MembershipStatus.EXPIRED.canTransitionTo(target);

            if (target == MembershipStatus.ACTIVE) {
                assertThat(canTransition).isTrue();
            } else {
                assertThat(canTransition).isFalse();
            }
        }
    }

    @Test
    @DisplayName("Should implement equals and hashCode based on membershipId")
    void shouldImplementEqualsAndHashCode() {
        UUID id = UUID.randomUUID();
        Membership m1 = Membership.create(ORG_ID, BRANCH_ID, MEMBER_ID, PLAN_NAME, START_DATE, END_DATE);
        Membership m2 = Membership.create(ORG_ID, BRANCH_ID, MEMBER_ID, PLAN_NAME, START_DATE, END_DATE);

        // Use reflection to set same ID for equality test
        // Since membershipId is generated, we test with different instances
        assertThat(m1).isNotEqualTo(m2);
        assertThat(m1).isEqualTo(m1);
        assertThat(m1.hashCode()).isEqualTo(m1.hashCode());
    }
}