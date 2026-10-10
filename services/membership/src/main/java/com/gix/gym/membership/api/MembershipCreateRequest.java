/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class MembershipCreateRequest {

    @NotNull(message = "Member ID is required")
    private UUID memberId;

    @NotBlank(message = "Plan name is required")
    @Size(max = 100, message = "Plan name must not exceed 100 characters")
    private String planName;

    @NotNull(message = "Start date is required")
    private Instant startDate;

    private Instant endDate;

    public MembershipCreateRequest() {
    }

    public MembershipCreateRequest(UUID memberId, String planName, Instant startDate, Instant endDate) {
        this.memberId = memberId;
        this.planName = planName;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public void setMemberId(UUID memberId) {
        this.memberId = memberId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MembershipCreateRequest)) return false;
        MembershipCreateRequest that = (MembershipCreateRequest) o;
        return Objects.equals(memberId, that.memberId) &&
                Objects.equals(planName, that.planName) &&
                Objects.equals(startDate, that.startDate) &&
                Objects.equals(endDate, that.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, planName, startDate, endDate);
    }

    @Override
    public String toString() {
        return "MembershipCreateRequest{" +
                "memberId=" + memberId +
                ", planName='" + planName + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}