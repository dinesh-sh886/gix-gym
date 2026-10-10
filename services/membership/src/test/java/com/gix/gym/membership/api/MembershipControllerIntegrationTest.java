/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.api;

import com.gix.gym.membership.domain.MembershipStatus;
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
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class MembershipControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
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
    }

    @Autowired
    private MockMvc mockMvc;

    private static final UUID ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BRANCH_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MEMBER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Instant START_DATE = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant END_DATE = Instant.parse("2026-12-31T23:59:59Z");
    private static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z");

    @Test
    @DisplayName("Should create membership via POST /api/v1/memberships")
    void shouldCreateMembership() throws Exception {
        String requestBody = """
                {
                    "memberId": "%s",
                    "planName": "Premium Monthly",
                    "startDate": "2026-01-01T00:00:00Z",
                    "endDate": "2026-12-31T23:59:59Z"
                }
                """.formatted(MEMBER_ID);

        mockMvc.perform(post("/api/v1/memberships")
                        .header("X-Organization-Id", ORG_ID.toString())
                        .header("X-Branch-Id", BRANCH_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.membershipId").exists())
                .andExpect(jsonPath("$.organizationId").value(ORG_ID.toString()))
                .andExpect(jsonPath("$.branchId").value(BRANCH_ID.toString()))
                .andExpect(jsonPath("$.memberId").value(MEMBER_ID.toString()))
                .andExpect(jsonPath("$.planName").value("Premium Monthly"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Should reject membership creation with missing memberId")
    void shouldRejectMissingMemberId() throws Exception {
        String requestBody = """
                {
                    "planName": "Premium Monthly",
                    "startDate": "2026-01-01T00:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/memberships")
                        .header("X-Organization-Id", ORG_ID.toString())
                        .header("X-Branch-Id", BRANCH_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.memberId").exists());
    }

    @Test
    @DisplayName("Should reject membership creation with blank planName")
    void shouldRejectBlankPlanName() throws Exception {
        String requestBody = """
                {
                    "memberId": "%s",
                    "planName": "",
                    "startDate": "2026-01-01T00:00:00Z"
                }
                """.formatted(MEMBER_ID);

        mockMvc.perform(post("/api/v1/memberships")
                        .header("X-Organization-Id", ORG_ID.toString())
                        .header("X-Branch-Id", BRANCH_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.planName").exists());
    }

    @Test
    @DisplayName("Should get membership by ID")
    void shouldGetMembershipById() throws Exception {
        // First create a membership
        String createBody = """
                {
                    "memberId": "%s",
                    "planName": "Premium Monthly",
                    "startDate": "2026-01-01T00:00:00Z",
                    "endDate": "2026-12-31T23:59:59Z"
                }
                """.formatted(MEMBER_ID);

        String createResponse = mockMvc.perform(post("/api/v1/memberships")
                        .header("X-Organization-Id", ORG_ID.toString())
                        .header("X-Branch-Id", BRANCH_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID membershipId = UUID.fromString(
                createResponse.substring(createResponse.indexOf("membershipId") + 15, createResponse.indexOf("membershipId") + 51));

        // Then get it
        mockMvc.perform(get("/api/v1/memberships/{membershipId}", membershipId)
                        .header("X-Organization-Id", ORG_ID.toString())
                        .header("X-Branch-Id", BRANCH_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipId").value(membershipId.toString()))
                .andExpect(jsonPath("$.planName").value("Premium Monthly"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Should return 404 for non-existent membership")
    void shouldReturn404ForNonExistentMembership() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/memberships/{membershipId}", nonExistentId)
                        .header("X-Organization-Id", ORG_ID.toString())
                        .header("X-Branch-Id", BRANCH_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Should return 404 when accessing membership from different organization")
    void shouldReturn404ForDifferentOrganization() throws Exception {
        String createBody = """
                {
                    "memberId": "%s",
                    "planName": "Premium Monthly",
                    "startDate": "2026-01-01T00:00:00Z",
                    "endDate": "2026-12-31T23:59:59Z"
                }
                """.formatted(MEMBER_ID);

        String createResponse = mockMvc.perform(post("/api/v1/memberships")
                        .header("X-Organization-Id", ORG_ID.toString())
                        .header("X-Branch-Id", BRANCH_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID membershipId = UUID.fromString(
                createResponse.substring(createResponse.indexOf("membershipId") + 15, createResponse.indexOf("membershipId") + 51));

        UUID otherOrgId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        mockMvc.perform(get("/api/v1/memberships/{membershipId}", membershipId)
                        .header("X-Organization-Id", otherOrgId.toString())
                        .header("X-Branch-Id", BRANCH_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Should list memberships with pagination")
    void shouldListMembershipsWithPagination() throws Exception {
        // Create two memberships
        String createBody1 = """
                {
                    "memberId": "%s",
                    "planName": "Premium",
                    "startDate": "2026-01-01T00:00:00Z",
                    "endDate": "2026-12-31T23:59:59Z"
                }
                """.formatted(MEMBER_ID);

        String createBody2 = """
                {
                    "memberId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                    "planName": "Basic",
                    "startDate": "2026-01-01T00:00:00Z",
                    "endDate": "2026-12-31T23:59:59Z"
                }
                """;

        mockMvc.perform(post("/api/v1/memberships")
                        .header("X-Organization-Id", ORG_ID.toString())
                        .header("X-Branch-Id", BRANCH_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody1))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/memberships")
                        .header("X-Organization-Id", ORG_ID.toString())
                        .header("X-Branch-Id", BRANCH_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody2))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/memberships")
                        .header("X-Organization-Id", ORG_ID.toString())
                        .header("X-Branch-Id", BRANCH_ID.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));
    }

    @Test
    @DisplayName("Should freeze membership")
    void shouldFreezeMembership() throws Exception {
        String createBody = """
                {
                    "memberId": "%s",
                    "planName": "Premium",
                    "startDate": "2026-01-01T00:00:00Z",
                    "endDate": "2026-12-31T23:59:59Z"
                }
                """.formatted(MEMBER_ID);

        String createResponse = mockMvc.perform(post("/api/v1/memberships")
                        .header("X-Organization-Id", ORG_ID.toString())
                        .header("X-Branch-Id", BRANCH_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID membershipId = UUID.fromString(
                createResponse.substring(createResponse.indexOf("membershipId") + 15, createResponse.indexOf("membershipId") + 51));

        String freezeBody = """
                {
                    "effectiveAt": "2026-06-15T12:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/memberships/{membershipId}/freeze", membershipId)
                        .header("X-Organization-Id", ORG_ID.toString())
                        .header("X-Branch-Id", BRANCH_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(freezeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"))
                .andExpect(jsonPath("$.frozenAt").value("2026-06-15T12:00:00Z"));
    }

    @Test
    @DisplayName("Should cancel membership")
    void shouldCancelMembership() throws Exception {
        String createBody = """
                {
                    "memberId": "%s",
                    "planName": "Premium",
                    "startDate": "2026-01-01T00:00:00Z",
                    "endDate": "2026-12-31T23:59:59Z"
                }
                """.formatted(MEMBER_ID);

        String createResponse = mockMvc.perform(post("/api/v1/memberships")
                        .header("X-Organization-Id", ORG_ID.toString())
                        .header("X-Branch-Id", BRANCH_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID membershipId = UUID.fromString(
                createResponse.substring(createResponse.indexOf("membershipId") + 15, createResponse.indexOf("membershipId") + 51));

        String cancelBody = """
                {
                    "effectiveAt": "2026-06-15T12:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/memberships/{membershipId}/cancel", membershipId)
                        .header("X-Organization-Id", ORG_ID.toString())
                        .header("X-Branch-Id", BRANCH_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").value("2026-06-15T12:00:00Z"));
    }

    @Test
    @DisplayName("Should reject freeze of cancelled membership")
    void shouldRejectFreezeOfCancelledMembership() throws Exception {
        String createBody = """
                {
                    "memberId": "%s",
                    "planName": "Premium",
                    "startDate": "2026-01-01T00:00:00Z",
                    "endDate": "2026-12-31T23:59:59Z"
                }
                """.formatted(MEMBER_ID);

        String createResponse = mockMvc.perform(post("/api/v1/memberships")
                        .header("X-Organization-Id", ORG_ID.toString())
                        .header("X-Branch-Id", BRANCH_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID membershipId = UUID.fromString(
                createResponse.substring(createResponse.indexOf("membershipId") + 15, createResponse.indexOf("membershipId") + 51));

        // Cancel first
        String cancelBody = """
                {
                    "effectiveAt": "2026-06-15T12:00:00Z"
                }
                """;
        mockMvc.perform(post("/api/v1/memberships/{membershipId}/cancel", membershipId)
                        .header("X-Organization-Id", ORG_ID.toString())
                        .header("X-Branch-Id", BRANCH_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelBody))
                .andExpect(status().isOk());

        // Try to freeze
        String freezeBody = """
                {
                    "effectiveAt": "2026-06-15T13:00:00Z"
                }
                """;
        mockMvc.perform(post("/api/v1/memberships/{membershipId}/freeze", membershipId)
                        .header("X-Organization-Id", ORG_ID.toString())
                        .header("X-Branch-Id", BRANCH_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(freezeBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cannot freeze membership from status: CANCELLED"));
    }
}