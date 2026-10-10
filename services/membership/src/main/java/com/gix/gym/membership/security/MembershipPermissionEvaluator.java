/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.security;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Component
public class MembershipPermissionEvaluator implements PermissionEvaluator {

    private static final Set<String> MEMBERSHIP_CREATE_PERMISSIONS = Set.of("membership:create", "membership:write");
    private static final Set<String> MEMBERSHIP_READ_PERMISSIONS = Set.of("membership:read", "membership:write");
    private static final Set<String> MEMBERSHIP_FREEZE_PERMISSIONS = Set.of("membership:freeze", "membership:write");
    private static final Set<String> MEMBERSHIP_CANCEL_PERMISSIONS = Set.of("membership:cancel", "membership:write");
    private static final Set<String> ADMIN_PERMISSIONS = Set.of("admin", "platform:admin");

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return false;
        }

        Jwt jwt = jwtAuth.getToken();
        List<String> permissions = jwt.getClaimAsStringList("permissions");
        List<String> roles = jwt.getClaimAsStringList("roles");

        if (permissions == null && roles == null) {
            return false;
        }

        String requiredPermission = permission.toString();

        if (hasAdminAccess(roles)) {
            return true;
        }

        if (permissions != null && permissions.contains(requiredPermission)) {
            return true;
        }

        return switch (requiredPermission) {
            case "membership:create" -> hasAnyPermission(permissions, MEMBERSHIP_CREATE_PERMISSIONS);
            case "membership:read" -> hasAnyPermission(permissions, MEMBERSHIP_READ_PERMISSIONS);
            case "membership:freeze" -> hasAnyPermission(permissions, MEMBERSHIP_FREEZE_PERMISSIONS);
            case "membership:cancel" -> hasAnyPermission(permissions, MEMBERSHIP_CANCEL_PERMISSIONS);
            default -> false;
        };
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return hasPermission(authentication, targetId, permission);
    }

    private boolean hasAdminAccess(List<String> roles) {
        if (roles == null) return false;
        return roles.stream().anyMatch(r -> ADMIN_PERMISSIONS.contains(r.toLowerCase()));
    }

    private boolean hasAnyPermission(List<String> permissions, Set<String> allowed) {
        if (permissions == null) return false;
        return permissions.stream().anyMatch(p -> allowed.contains(p.toLowerCase()));
    }
}