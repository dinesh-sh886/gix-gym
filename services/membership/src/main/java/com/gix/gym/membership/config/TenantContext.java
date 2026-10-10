/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.config;

import java.util.UUID;

public record TenantContext(UUID organizationId, UUID branchId) {

    public static final String ORGANIZATION_ID_ATTRIBUTE = "TENANT_ORGANIZATION_ID";
    public static final String BRANCH_ID_ATTRIBUTE = "TENANT_BRANCH_ID";

    public static TenantContext fromRequest(jakarta.servlet.http.HttpServletRequest request) {
        UUID organizationId = (UUID) request.getAttribute(ORGANIZATION_ID_ATTRIBUTE);
        UUID branchId = (UUID) request.getAttribute(BRANCH_ID_ATTRIBUTE);

        if (organizationId == null || branchId == null) {
            throw new IllegalStateException("Tenant context not found in request. Ensure JWT authentication filter is applied.");
        }

        return new TenantContext(organizationId, branchId);
    }
}