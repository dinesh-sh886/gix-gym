/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.api;

import com.gix.gym.membership.application.MembershipService;
import com.gix.gym.membership.config.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/memberships")
@Tag(name = "Memberships", description = "Membership management API")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping
    @Operation(summary = "Create a new membership",
            description = "Creates a new membership for a member within the authenticated organization and branch")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Membership created",
                    content = @Content(schema = @Schema(implementation = MembershipResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Member already has active membership",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MembershipResponse> createMembership(
            @Valid @RequestBody MembershipCreateRequest request) {

        UUID organizationId = TenantContextHolder.getOrganizationId();
        UUID branchId = TenantContextHolder.getBranchId();
        MembershipResponse response = membershipService.createMembership(
                organizationId,
                branchId,
                request.getMemberId(),
                request.getPlanName(),
                request.getStartDate(),
                request.getEndDate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{membershipId}")
    @Operation(summary = "Get membership by ID",
            description = "Retrieves a membership by its ID within the authenticated organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Membership found",
                    content = @Content(schema = @Schema(implementation = MembershipResponse.class))),
            @ApiResponse(responseCode = "404", description = "Membership not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MembershipResponse> getMembership(
            @Parameter(description = "Membership UUID") @PathVariable UUID membershipId) {

        UUID organizationId = TenantContextHolder.getOrganizationId();
        MembershipResponse response = membershipService.getMembership(organizationId, membershipId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List memberships",
            description = "Lists memberships for the authenticated organization with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Memberships retrieved",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<MembershipResponse>> listMemberships(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir) {

        UUID organizationId = TenantContextHolder.getOrganizationId();
        UUID branchId = TenantContextHolder.getBranchId();
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MembershipResponse> membershipPage = membershipService.listMemberships(
                organizationId, branchId, pageable);

        return ResponseEntity.ok(PageResponse.of(membershipPage));
    }

    @PostMapping("/{membershipId}/freeze")
    @Operation(summary = "Freeze a membership",
            description = "Freezes an active membership")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Membership frozen",
                    content = @Content(schema = @Schema(implementation = MembershipResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid state transition",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Membership not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MembershipResponse> freezeMembership(
            @Parameter(description = "Membership UUID") @PathVariable UUID membershipId,
            @Valid @RequestBody LifecycleActionRequest request) {

        UUID organizationId = TenantContextHolder.getOrganizationId();
        MembershipResponse response = membershipService.freezeMembership(
                organizationId, membershipId, request.getEffectiveAt());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{membershipId}/cancel")
    @Operation(summary = "Cancel a membership",
            description = "Cancels an active or frozen membership")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Membership cancelled",
                    content = @Content(schema = @Schema(implementation = MembershipResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid state transition",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Membership not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MembershipResponse> cancelMembership(
            @Parameter(description = "Membership UUID") @PathVariable UUID membershipId,
            @Valid @RequestBody LifecycleActionRequest request) {

        UUID organizationId = TenantContextHolder.getOrganizationId();
        MembershipResponse response = membershipService.cancelMembership(
                organizationId, membershipId, request.getEffectiveAt());
        return ResponseEntity.ok(response);
    }
}