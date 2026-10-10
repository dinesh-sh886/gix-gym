/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Transaction synchronization to set PostgreSQL RLS context.
 * Sets `SET LOCAL app.current_org_id` at transaction start and clears it at transaction end.
 */
@Component
public class RlsTransactionSynchronization {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Registers RLS context setup for the current transaction.
     * Should be called when tenant context is established (e.g., in JwtAuthenticationFilter).
     */
    public void registerRlsContext() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // No active transaction management - cannot set RLS context safely
            return;
        }

        UUID organizationId = TenantContextHolder.getOrganizationId();
        if (organizationId == null) {
            throw new IllegalStateException("Cannot register RLS context: organizationId not set in TenantContextHolder");
        }

        // Set the RLS context immediately for the current transaction
        setRlsContext(organizationId);

        // Register cleanup for transaction completion
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                clearRlsContext();
            }
        });
    }

    private void setRlsContext(UUID organizationId) {
        try {
            entityManager.createNativeQuery("SET LOCAL app.current_org_id = :orgId")
                    .setParameter("orgId", organizationId.toString())
                    .executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set RLS context for organization: " + organizationId, e);
        }
    }

    private void clearRlsContext() {
        try {
            entityManager.createNativeQuery("RESET app.current_org_id").executeUpdate();
        } catch (Exception e) {
            // Log but don't throw - transaction is already completing
            System.err.println("Warning: Failed to clear RLS context: " + e.getMessage());
        }
    }
}