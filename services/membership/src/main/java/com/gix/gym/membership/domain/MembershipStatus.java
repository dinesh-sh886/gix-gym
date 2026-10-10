/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.domain;

import java.util.Arrays;

public enum MembershipStatus {

    ACTIVE("Active membership"),
    FROZEN("Temporarily frozen membership"),
    CANCELLED("Cancelled membership"),
    EXPIRED("Expired membership");

    private final String description;

    MembershipStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(MembershipStatus target) {
        return switch (this) {
            case ACTIVE -> target == FROZEN || target == CANCELLED || target == EXPIRED;
            case FROZEN -> target == ACTIVE || target == CANCELLED;
            case CANCELLED -> false;
            case EXPIRED -> target == ACTIVE;
        };
    }

    public static MembershipStatus fromString(String value) {
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid membership status: " + value));
    }
}