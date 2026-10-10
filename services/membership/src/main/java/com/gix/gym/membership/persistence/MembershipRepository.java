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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    Optional<Membership> findByMembershipIdAndOrganizationId(UUID membershipId, UUID organizationId);

    @Query("SELECT m FROM Membership m WHERE m.organizationId = :organizationId AND m.branchId = :branchId")
    Page<Membership> findByOrganizationIdAndBranchId(@Param("organizationId") UUID organizationId,
                                                      @Param("branchId") UUID branchId,
                                                      Pageable pageable);

    @Query("SELECT m FROM Membership m WHERE m.organizationId = :organizationId AND m.memberId = :memberId")
    List<Membership> findByOrganizationIdAndMemberId(@Param("organizationId") UUID organizationId,
                                                      @Param("memberId") UUID memberId);

    @Query("SELECT m FROM Membership m WHERE m.organizationId = :organizationId AND m.status = :status")
    List<Membership> findByOrganizationIdAndStatus(@Param("organizationId") UUID organizationId,
                                                    @Param("status") MembershipStatus status);

    boolean existsByOrganizationIdAndMemberIdAndStatusIn(UUID organizationId, UUID memberId,
                                                          List<MembershipStatus> statuses);

    @Query("SELECT COUNT(m) FROM Membership m WHERE m.organizationId = :organizationId")
    long countByOrganizationId(@Param("organizationId") UUID organizationId);
}