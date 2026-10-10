/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.application;

import com.gix.gym.membership.api.MembershipResponse;
import com.gix.gym.membership.domain.Membership;
import com.gix.gym.membership.domain.MembershipStatus;
import com.gix.gym.membership.domain.OutboxEvent;
import com.gix.gym.membership.persistence.MembershipRepository;
import com.gix.gym.membership.persistence.OutboxEventRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final OutboxEventRepository outboxEventRepository;

    public MembershipService(MembershipRepository membershipRepository,
                             OutboxEventRepository outboxEventRepository) {
        this.membershipRepository = membershipRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public MembershipResponse createMembership(UUID organizationId, UUID branchId,
                                               UUID memberId, String planName,
                                               Instant startDate, Instant endDate) {

        List<MembershipStatus> activeStatuses = List.of(MembershipStatus.ACTIVE, MembershipStatus.FROZEN);
        if (membershipRepository.existsByOrganizationIdAndMemberIdAndStatusIn(
                organizationId, memberId, activeStatuses)) {
            throw new IllegalStateException("Member already has an active or frozen membership");
        }

        Membership membership = Membership.create(organizationId, branchId, memberId,
                planName, startDate, endDate);
        membership = membershipRepository.save(membership);

        // Create outbox event for MembershipCreated
        Map<String, Object> payload = buildMembershipCreatedPayload(membership);
        OutboxEvent outboxEvent = OutboxEvent.create(
                "membership.created",
                "1.0",
                "Membership",
                membership.getMembershipId().toString(),
                organizationId,
                Instant.now(),
                payload,
                null
        );
        outboxEventRepository.save(outboxEvent);

        return toResponse(membership);
    }

    @Transactional(readOnly = true)
    public MembershipResponse getMembership(UUID organizationId, UUID membershipId) {
        Membership membership = membershipRepository.findByMembershipIdAndOrganizationId(membershipId, organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found: " + membershipId));
        return toResponse(membership);
    }

    @Transactional(readOnly = true)
    public Page<MembershipResponse> listMemberships(UUID organizationId, UUID branchId, Pageable pageable) {
        return membershipRepository.findByOrganizationIdAndBranchId(organizationId, branchId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public MembershipResponse freezeMembership(UUID organizationId, UUID membershipId, Instant effectiveAt) {
        Membership membership = membershipRepository.findByMembershipIdAndOrganizationId(membershipId, organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found: " + membershipId));

        membership.freeze(effectiveAt);
        membership = membershipRepository.save(membership);

        OutboxEvent outboxEvent = OutboxEvent.create(
                "membership.frozen",
                "1.0",
                "Membership",
                membership.getMembershipId().toString(),
                organizationId,
                effectiveAt,
                buildMembershipFrozenPayload(membership),
                null
        );
        outboxEventRepository.save(outboxEvent);

        return toResponse(membership);
    }

    @Transactional
    public MembershipResponse cancelMembership(UUID organizationId, UUID membershipId, Instant effectiveAt) {
        Membership membership = membershipRepository.findByMembershipIdAndOrganizationId(membershipId, organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found: " + membershipId));

        membership.cancel(effectiveAt);
        membership = membershipRepository.save(membership);

        OutboxEvent outboxEvent = OutboxEvent.create(
                "membership.cancelled",
                "1.0",
                "Membership",
                membership.getMembershipId().toString(),
                organizationId,
                effectiveAt,
                buildMembershipCancelledPayload(membership),
                null
        );
        outboxEventRepository.save(outboxEvent);

        return toResponse(membership);
    }

    private MembershipResponse toResponse(Membership membership) {
        return new MembershipResponse(
                membership.getMembershipId(),
                membership.getOrganizationId(),
                membership.getBranchId(),
                membership.getMemberId(),
                membership.getPlanName(),
                membership.getStartDate(),
                membership.getEndDate(),
                membership.getStatus(),
                membership.getFrozenAt(),
                membership.getCancelledAt(),
                membership.getCreatedAt(),
                membership.getUpdatedAt()
        );
    }

    private Map<String, Object> buildMembershipCreatedPayload(Membership membership) {
        return Map.of(
                "membershipId", membership.getMembershipId().toString(),
                "organizationId", membership.getOrganizationId().toString(),
                "branchId", membership.getBranchId().toString(),
                "memberId", membership.getMemberId().toString(),
                "planName", membership.getPlanName(),
                "startDate", membership.getStartDate().toString(),
                "endDate", membership.getEndDate() != null ? membership.getEndDate().toString() : null,
                "status", membership.getStatus().name()
        );
    }

    private Map<String, Object> buildMembershipFrozenPayload(Membership membership) {
        return Map.of(
                "membershipId", membership.getMembershipId().toString(),
                "organizationId", membership.getOrganizationId().toString(),
                "frozenAt", membership.getFrozenAt().toString()
        );
    }

    private Map<String, Object> buildMembershipCancelledPayload(Membership membership) {
        return Map.of(
                "membershipId", membership.getMembershipId().toString(),
                "organizationId", membership.getOrganizationId().toString(),
                "cancelledAt", membership.getCancelledAt().toString()
        );
    }
}