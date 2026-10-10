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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MembershipStatusTest {

    @Test
    @DisplayName("Should parse valid status strings case-insensitively")
    void shouldParseValidStatusStrings() {
        assertThat(MembershipStatus.fromString("ACTIVE")).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(MembershipStatus.fromString("active")).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(MembershipStatus.fromString("Active")).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(MembershipStatus.fromString("FROZEN")).isEqualTo(MembershipStatus.FROZEN);
        assertThat(MembershipStatus.fromString("CANCELLED")).isEqualTo(MembershipStatus.CANCELLED);
        assertThat(MembershipStatus.fromString("EXPIRED")).isEqualTo(MembershipStatus.EXPIRED);
    }

    @Test
    @DisplayName("Should throw for invalid status string")
    void shouldThrowForInvalidStatus() {
        assertThatThrownBy(() -> MembershipStatus.fromString("INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid membership status: INVALID");
    }

    @Test
    @DisplayName("Should throw for null status string")
    void shouldThrowForNullStatus() {
        assertThatThrownBy(() -> MembershipStatus.fromString(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should have descriptions")
    void shouldHaveDescriptions() {
        assertThat(MembershipStatus.ACTIVE.getDescription()).isEqualTo("Active membership");
        assertThat(MembershipStatus.FROZEN.getDescription()).isEqualTo("Temporarily frozen membership");
        assertThat(MembershipStatus.CANCELLED.getDescription()).isEqualTo("Cancelled membership");
        assertThat(MembershipStatus.EXPIRED.getDescription()).isEqualTo("Expired membership");
    }
}