/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final RlsTransactionSynchronization rlsTransactionSynchronization;

    public JwtAuthenticationFilter(RlsTransactionSynchronization rlsTransactionSynchronization) {
        this.rlsTransactionSynchronization = rlsTransactionSynchronization;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();

                // Extract tenant context from JWT claims
                String orgIdClaim = jwt.getClaimAsString("org_id");
                String branchIdClaim = jwt.getClaimAsString("branch_id");

                if (orgIdClaim != null && branchIdClaim != null) {
                    UUID organizationId = UUID.fromString(orgIdClaim);
                    UUID branchId = UUID.fromString(branchIdClaim);

                    // Store tenant context in request attribute for downstream use
                    request.setAttribute(TenantContext.ORGANIZATION_ID_ATTRIBUTE, organizationId);
                    request.setAttribute(TenantContext.BRANCH_ID_ATTRIBUTE, branchId);

                    // Also set in ThreadLocal for service layer access
                    TenantContextHolder.setContext(new TenantContext(organizationId, branchId));

                    // Register RLS context for current transaction
                    rlsTransactionSynchronization.registerRlsContext();
                }
            }
        } catch (Exception e) {
            // Log but don't block - let the security framework handle authentication errors
            // Invalid tokens will be rejected by the OAuth2 resource server
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clear ThreadLocal context after request completes
            TenantContextHolder.clear();
        }
    }
}