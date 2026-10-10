/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.config;

import java.util.UUID;

/**
 * Request-scoped holder for tenant context.
 * Populated by JwtAuthenticationFilter and cleared at the end of request.
 */
public class TenantContextHolder {

    private static final ThreadLocal<TenantContext> contextHolder = new ThreadLocal<>();

    private TenantContextHolder() {
        // Utility class
    }

    public static void setContext(TenantContext context) {
        contextHolder.set(context);
    }

    public static TenantContext getContext() {
        TenantContext context = contextHolder.get();
        if (context == null) {
            throw new IllegalStateException("Tenant context not set. Ensure request is authenticated and JWT filter has run.");
        }
        return context;
    }

    public static UUID getOrganizationId() {
        return getContext().organizationId();
    }

    public static UUID getBranchId() {
        return getContext().branchId();
    }

    public static void clear() {
        contextHolder.remove();
    }

    public static boolean hasContext() {
        return contextHolder.get() != null;
    }
}