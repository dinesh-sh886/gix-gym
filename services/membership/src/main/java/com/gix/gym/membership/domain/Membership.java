/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "membership",
        indexes = {
                @Index(name = "idx_membership_org_id", columnList = "organization_id"),
                @Index(name = "idx_membership_branch_id", columnList = "branch_id"),
                @Index(name = "idx_membership_status", columnList = "status"),
                @Index(name = "idx_membership_member_id", columnList = "member_id")
        })
public class Membership {

    @Id
    @Column(name = "membership_id", nullable = false, updatable = false)
    private UUID membershipId;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "branch_id", nullable = false, updatable = false)
    private UUID branchId;

    @Column(name = "member_id", nullable = false, updatable = false)
    private UUID memberId;

    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MembershipStatus status;

    @Column(name = "frozen_at")
    private Instant frozenAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Membership() {
        // JPA required
    }

    private Membership(UUID membershipId, UUID organizationId, UUID branchId,
                       UUID memberId, String planName, Instant startDate,
                       Instant endDate, MembershipStatus status) {
        this.membershipId = membershipId;
        this.organizationId = organizationId;
        this.branchId = branchId;
        this.memberId = memberId;
        this.planName = planName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.version = 0L;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static Membership create(UUID organizationId, UUID branchId,
                                    UUID memberId, String planName,
                                    Instant startDate, Instant endDate) {
        return new Membership(UUID.randomUUID(), organizationId, branchId,
                memberId, planName, startDate, endDate, MembershipStatus.ACTIVE);
    }

    public void freeze(Instant frozenAt) {
        if (!status.canTransitionTo(MembershipStatus.FROZEN)) {
            throw new IllegalStateException("Cannot freeze membership from status: " + status);
        }
        this.status = MembershipStatus.FROZEN;
        this.frozenAt = frozenAt;
        this.updatedAt = Instant.now();
    }

    public void activate(Instant activatedAt) {
        if (!status.canTransitionTo(MembershipStatus.ACTIVE)) {
            throw new IllegalStateException("Cannot activate membership from status: " + status);
        }
        this.status = MembershipStatus.ACTIVE;
        this.frozenAt = null;
        this.updatedAt = activatedAt;
    }

    public void cancel(Instant cancelledAt) {
        if (!status.canTransitionTo(MembershipStatus.CANCELLED)) {
            throw new IllegalStateException("Cannot cancel membership from status: " + status);
        }
        this.status = MembershipStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
        this.updatedAt = cancelledAt;
    }

    public void expire(Instant expiredAt) {
        if (!status.canTransitionTo(MembershipStatus.EXPIRED)) {
            throw new IllegalStateException("Cannot expire membership from status: " + status);
        }
        this.status = MembershipStatus.EXPIRED;
        this.updatedAt = expiredAt;
    }

    // Getters
    public UUID getMembershipId() {
        return membershipId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public String getPlanName() {
        return planName;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public Instant getFrozenAt() {
        return frozenAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Membership)) return false;
        Membership that = (Membership) o;
        return Objects.equals(membershipId, that.membershipId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(membershipId);
    }

    @Override
    public String toString() {
        return "Membership{" +
                "membershipId=" + membershipId +
                ", organizationId=" + organizationId +
                ", branchId=" + branchId +
                ", memberId=" + memberId +
                ", planName='" + planName + '\'' +
                ", status=" + status +
                '}';
    }
}