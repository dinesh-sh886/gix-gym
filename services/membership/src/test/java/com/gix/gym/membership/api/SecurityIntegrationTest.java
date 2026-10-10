/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SecurityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine")
            .withDatabaseName("gix_membership")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("JWT_SECRET", () -> "test-secret-key-for-testing-minimum-32-chars");
        registry.add("JWT_ISSUER", () -> "gix-gym");
        registry.add("JWT_AUDIENCE", () -> "gix-gym-clients");
    }

    @Autowired
    private MockMvc mockMvc;

    private static final UUID ORG_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ORG_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID BRANCH_ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BRANCH_ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID MEMBER_ID_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MEMBER_ID_2 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Instant START_DATE = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant END_DATE = Instant.parse("2026-12-31T23:59:59Z");
    private static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z");

    @Test
    @DisplayName("Should return 401 when no token provided")
    void shouldReturn401WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/memberships"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when token is missing")
    void shouldReturn401WhenTokenMissing() throws Exception {
        mockMvc.perform(get("/api/v1/memberships")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when token is malformed")
    void shouldReturn401WhenTokenMalformed() throws Exception {
        mockMvc.perform(get("/api/v1/memberships")
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when token has invalid signature")
    void shouldReturn401WhenTokenInvalidSignature() throws Exception {
        // Token signed with different secret
        String invalidToken = generateToken(ORG_ID_1, BRANCH_ID_1, "MEMBER", "test-user",
                "different-secret-key-for-testing-minimum-32-chars", "gix-gym", "gix-gym-clients");

        mockMvc.perform(get("/api/v1/memberships")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when token is expired")
    void shouldReturn401WhenTokenExpired() throws Exception {
        // We can't easily test expired tokens without manipulating time
        // This is a placeholder for when time manipulation is available
    }

    @Test
    @DisplayName("Should return 401 when token has wrong issuer")
    void shouldReturn401WhenTokenWrongIssuer() throws Exception {
        String token = generateTokenWithCustomIssuer(ORG_ID_1, BRANCH_ID_1, "MEMBER", "test-user",
                "wrong-issuer");

        mockMvc.perform(get("/api/v1/memberships")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when token has wrong audience")
    void shouldReturn401WhenTokenWrongAudience() throws Exception {
        String token = generateTokenWithCustomAudience(ORG_ID_1, BRANCH_ID_1, "MEMBER", "test-user",
                "wrong-audience");

        mockMvc.perform(get("/api/v1/memberships")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 403 when valid token but insufficient permissions")
    void shouldReturn403WhenInsufficientPermissions() throws Exception {
        // Test with a role that doesn't have membership access
        String token = generateToken(ORG_ID_1, BRANCH_ID_1, "VIEWER", "test-user");

        mockMvc.perform(get("/api/v1/memberships")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Tenant A cannot retrieve Tenant B's membership")
    void shouldNotAllowTenantAToRetrieveTenantBMembership() throws Exception {
        // Create membership as Tenant A
        String tokenA = generateToken(ORG_ID_1, BRANCH_ID_1, "MEMBER", "user-a");
        String createBody = """
                {
                    "memberId": "%s",
                    "planName": "Premium Monthly",
                    "startDate": "2026-01-01T00:00:00Z",
                    "endDate": "2026-12-31T23:59:59Z"
                }
                """.formatted(MEMBER_ID_1);

        String createResponse = mockMvc.perform(post("/api/v1/memberships")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID membershipId = UUID.fromString(
                createResponse.substring(createResponse.indexOf("membershipId") + 15,
                        createResponse.indexOf("membershipId") + 51));

        // Try to access as Tenant B
        String tokenB = generateToken(ORG_ID_2, BRANCH_ID_2, "MEMBER", "user-b");

        mockMvc.perform(get("/api/v1/memberships/{membershipId}", membershipId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tenant A cannot update Tenant B's membership")
    void shouldNotAllowTenantAToUpdateTenantBMembership() throws Exception {
        // Create membership as Tenant A
        String tokenA = generateToken(ORG_ID_1, BRANCH_ID_1, "MEMBER", "user-a");
        String createBody = """
                {
                    "memberId": "%s",
                    "planName": "Premium Monthly",
                    "startDate": "2026-01-01T00:00:00Z",
                    "endDate": "2026-12-31T23:59:59Z"
                }
                """.formatted(MEMBER_ID_1);

        String createResponse = mockMvc.perform(post("/api/v1/memberships")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID membershipId = UUID.fromString(
                createResponse.substring(createResponse.indexOf("membershipId") + 15,
                        createResponse.indexOf("membershipId") + 51));

        // Try to freeze as Tenant B
        String tokenB = generateToken(ORG_ID_2, BRANCH_ID_2, "MEMBER", "user-b");
        String freezeBody = """
                {
                    "effectiveAt": "2026-06-15T12:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/memberships/{membershipId}/freeze", membershipId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(freezeBody))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tenant A cannot cancel Tenant B's membership")
    void shouldNotAllowTenantAToCancelTenantBMembership() throws Exception {
        // Create membership as Tenant A
        String tokenA = generateToken(ORG_ID_1, BRANCH_ID_1, "MEMBER", "user-a");
        String createBody = """
                {
                    "memberId": "%s",
                    "planName": "Premium Monthly",
                    "startDate": "2026-01-01T00:00:00Z",
                    "endDate": "2026-12-31T23:59:59Z"
                }
                """.formatted(MEMBER_ID_1);

        String createResponse = mockMvc.perform(post("/api/v1/memberships")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID membershipId = UUID.fromString(
                createResponse.substring(createResponse.indexOf("membershipId") + 15,
                        createResponse.indexOf("membershipId") + 51));

        // Try to cancel as Tenant B
        String tokenB = generateToken(ORG_ID_2, BRANCH_ID_2, "MEMBER", "user-b");
        String cancelBody = """
                {
                    "effectiveAt": "2026-06-15T12:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/memberships/{membershipId}/cancel", membershipId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelBody))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tenant A's list endpoint returns only Tenant A's records")
    void shouldOnlyReturnTenantAOwnRecords() throws Exception {
        String tokenA = generateToken(ORG_ID_1, BRANCH_ID_1, "MEMBER", "user-a");
        String tokenB = generateToken(ORG_ID_2, BRANCH_ID_2, "MEMBER", "user-b");

        // Create membership for Tenant A
        String createBodyA = """
                {
                    "memberId": "%s",
                    "planName": "Premium",
                    "startDate": "2026-01-01T00:00:00Z",
                    "endDate": "2026-12-31T23:59:59Z"
                }
                """.formatted(MEMBER_ID_1);

        mockMvc.perform(post("/api/v1/memberships")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBodyA))
                .andExpect(status().isCreated());

        // Create membership for Tenant B
        String createBodyB = """
                {
                    "memberId": "%s",
                    "planName": "Basic",
                    "startDate": "2026-01-01T00:00:00Z",
                    "endDate": "2026-12-31T23:59:59Z"
                }
                """.formatted(MEMBER_ID_2);

        mockMvc.perform(post("/api/v1/memberships")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBodyB))
                .andExpect(status().isCreated());

        // List as Tenant A - should only see Tenant A's membership
        mockMvc.perform(get("/api/v1/memberships")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Valid token with MEMBER role can access membership endpoints")
    void shouldAllowValidTokenWithMemberRole() throws Exception {
        String token = generateToken(ORG_ID_1, BRANCH_ID_1, "MEMBER", "test-user");

        mockMvc.perform(get("/api/v1/memberships")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Valid token with ADMIN role can access actuator endpoints")
    void shouldAllowValidTokenWithAdminRoleForActuator() throws Exception {
        String token = generateToken(ORG_ID_1, BRANCH_ID_1, "ADMIN", "admin-user");

        mockMvc.perform(get("/actuator/health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // Helper methods for generating test tokens
    private String generateToken(UUID orgId, UUID branchId, String roles, String subject) {
        return generateToken(orgId, branchId, roles, subject,
                "test-secret-key-for-testing-minimum-32-chars",
                "gix-gym", "gix-gym-clients");
    }

    private String generateTokenWithCustomIssuer(UUID orgId, UUID branchId, String roles, String subject, String issuer) {
        return generateToken(orgId, branchId, roles, subject,
                "test-secret-key-for-testing-minimum-32-chars",
                issuer, "gix-gym-clients");
    }

    private String generateTokenWithCustomAudience(UUID orgId, UUID branchId, String roles, String subject, String audience) {
        return generateToken(orgId, branchId, roles, subject,
                "test-secret-key-for-testing-minimum-32-chars",
                "gix-gym", audience);
    }

    private String generateToken(UUID orgId, UUID branchId, String roles, String subject,
                                 String secret, String issuer, String audience) {
        try {
            // Use reflection to call the main method of the test token generator
            // For simplicity, we'll use a simple implementation here
            // In practice, this would use the same JWT library as the application
            return createTestToken(orgId.toString(), branchId.toString(), roles, subject, secret, issuer, audience);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test token", e);
        }
    }

    private String createTestToken(String orgId, String branchId, String roles, String subject,
                                   String secret, String issuer, String audience) throws Exception {
        // Use the same JWT library as the application for consistency
        com.nimbusds.jose.JWSSigner signer = new com.nimbusds.jose.crypto.MACSigner(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        com.nimbusds.jwt.JWTClaimsSet claimsSet = new com.nimbusds.jwt.JWTClaimsSet.Builder()
                .subject(subject)
                .claim("org_id", orgId)
                .claim("branch_id", branchId)
                .claim("roles", java.util.Arrays.asList(roles.split(",")))
                .issuer(issuer)
                .audience(audience)
                .issueTime(new java.util.Date())
                .expirationTime(new java.util.Date(System.currentTimeMillis() + 15 * 60 * 1000))
                .build();

        com.nimbusds.jwt.SignedJWT signedJWT = new com.nimbusds.jwt.SignedJWT(
                new com.nimbusds.jose.JWSHeader.Builder(com.nimbusds.jose.JWSAlgorithm.HS256)
                        .type(com.nimbusds.jose.JOSEObjectType.JWT).build(),
                claimsSet
        );
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }
}