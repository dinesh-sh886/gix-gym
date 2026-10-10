/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.api;

import com.gix.gym.membership.domain.MembershipStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class MembershipResponse {

    private UUID membershipId;
    private UUID organizationId;
    private UUID branchId;
    private UUID memberId;
    private String planName;
    private Instant startDate;
    private Instant endDate;
    private MembershipStatus status;
    private Instant frozenAt;
    private Instant cancelledAt;
    private Instant createdAt;
    private Instant updatedAt;

    public MembershipResponse() {
    }

    public MembershipResponse(UUID membershipId, UUID organizationId, UUID branchId,
                              UUID memberId, String planName, Instant startDate,
                              Instant endDate, MembershipStatus status,
                              Instant frozenAt, Instant cancelledAt,
                              Instant createdAt, Instant updatedAt) {
        this.membershipId = membershipId;
        this.organizationId = organizationId;
        this.branchId = branchId;
        this.memberId = memberId;
        this.planName = planName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.frozenAt = frozenAt;
        this.cancelledAt = cancelledAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public UUID getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(UUID membershipId) {
        this.membershipId = membershipId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
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

    public MembershipStatus getStatus() {
        return status;
    }

    public void setStatus(MembershipStatus status) {
        this.status = status;
    }

    public Instant getFrozenAt() {
        return frozenAt;
    }

    public void setFrozenAt(Instant frozenAt) {
        this.frozenAt = frozenAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MembershipResponse)) return false;
        MembershipResponse that = (MembershipResponse) o;
        return Objects.equals(membershipId, that.membershipId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(membershipId);
    }

    @Override
    public String toString() {
        return "MembershipResponse{" +
                "membershipId=" + membershipId +
                ", organizationId=" + organizationId +
                ", branchId=" + branchId +
                ", memberId=" + memberId +
                ", planName='" + planName + '\'' +
                ", status=" + status +
                '}';
    }
}