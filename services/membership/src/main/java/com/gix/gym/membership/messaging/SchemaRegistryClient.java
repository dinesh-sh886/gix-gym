/*
 * Gix Gym
 * Copyright (C) 2026 dinesh-sh886
 *
 * This file is part of the Gix Gym project.
 * See the LICENSE file for licensing information (pending).
 */
package com.gix.gym.membership.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SchemaRegistryClient {

    private static final Logger log = LoggerFactory.getLogger(SchemaRegistryClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String schemaRegistryUrl;
    private final ConcurrentHashMap<String, Schema> schemaCache = new ConcurrentHashMap<>();

    public SchemaRegistryClient(@Value("${spring.kafka.schema-registry-url:http://localhost:8081}") String schemaRegistryUrl,
                                ObjectMapper objectMapper) {
        this.schemaRegistryUrl = schemaRegistryUrl;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public Schema getSchema(String eventType, String schemaVersion) {
        String cacheKey = eventType + "-" + schemaVersion;
        return schemaCache.computeIfAbsent(cacheKey, k -> fetchSchema(eventType, schemaVersion));
    }

    private Schema fetchSchema(String eventType, String schemaVersion) {
        try {
            String subject = eventType + "-value";
            String url = schemaRegistryUrl + "/subjects/" + subject + "/versions/" + schemaVersion;

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
            JsonNode json = objectMapper.readTree(response);
            String schemaString = json.get("schema").asText();

            return new Schema.Parser().parse(schemaString);
        } catch (Exception e) {
            log.error("Failed to fetch schema for {} version {}: {}", eventType, schemaVersion, e.getMessage());
            throw new IllegalStateException("Schema not found for " + eventType + " version " + schemaVersion, e);
        }
    }

    public int registerSchema(String eventType, String schemaJson) {
        try {
            String subject = eventType + "-value";
            String url = schemaRegistryUrl + "/subjects/" + subject + "/versions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = "{\"schema\": " + objectMapper.writeValueAsString(schemaJson) + "}";
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            String response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
            JsonNode json = objectMapper.readTree(response);
            return json.get("id").asInt();
        } catch (Exception e) {
            log.error("Failed to register schema for {}: {}", eventType, e.getMessage());
            throw new IllegalStateException("Failed to register schema for " + eventType, e);
        }
    }

    public Optional<Integer> getLatestSchemaId(String eventType) {
        try {
            String subject = eventType + "-value";
            String url = schemaRegistryUrl + "/subjects/" + subject + "/versions/latest";

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
            JsonNode json = objectMapper.readTree(response);
            return Optional.of(json.get("id").asInt());
        } catch (Exception e) {
            log.debug("No schema found for {}: {}", eventType, e.getMessage());
            return Optional.empty();
        }
    }
}