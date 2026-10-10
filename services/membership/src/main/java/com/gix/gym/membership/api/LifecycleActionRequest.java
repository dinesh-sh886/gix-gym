/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.api;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;

public class LifecycleActionRequest {

    @NotNull(message = "Effective timestamp is required")
    private Instant effectiveAt;

    public LifecycleActionRequest() {
    }

    public LifecycleActionRequest(Instant effectiveAt) {
        this.effectiveAt = effectiveAt;
    }

    public Instant getEffectiveAt() {
        return effectiveAt;
    }

    public void setEffectiveAt(Instant effectiveAt) {
        this.effectiveAt = effectiveAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LifecycleActionRequest)) return false;
        LifecycleActionRequest that = (LifecycleActionRequest) o;
        return Objects.equals(effectiveAt, that.effectiveAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(effectiveAt);
    }

    @Override
    public String toString() {
        return "LifecycleActionRequest{" +
                "effectiveAt=" + effectiveAt +
                '}';
    }
}