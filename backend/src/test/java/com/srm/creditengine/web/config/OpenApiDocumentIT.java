package com.srm.creditengine.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The OpenAPI document is the contract consumed by the frontend type generator: response fields must
 * be required unless nullable, and every documented error must be an RFC 9457 problem.
 */
@IntegrationTest
class OpenApiDocumentIT {

    private static final String PROBLEM_REF = "#/components/schemas/Problem";

    @Autowired
    private RestTestClient client;

    @Autowired
    private ObjectMapper mapper;

    private String raw;
    private JsonNode document;

    @BeforeEach
    void loadDocument() {
        raw = client.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        document = mapper.readTree(raw);
    }

    private JsonNode schema(String name) {
        return document.path("components").path("schemas").path(name);
    }

    private JsonNode responses(String path, String method) {
        return document.path("paths").path(path).path(method).path("responses");
    }

    private static List<String> texts(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.asString()));
        return values;
    }

    @Test
    void responseFieldsAreAlwaysPresentAndOnlyAnnotatedOnesAreNullable() {
        JsonNode operation = schema("CreditAssignmentResponse");
        assertThat(texts(operation.path("required")))
                .contains("id", "status", "version", "assignor", "totalNetAmount", "receivables", "createdAt")
                .contains("settledAt", "cancelledAt");
        assertThat(operation.path("properties").path("createdAt").has("oneOf")).isFalse();
        assertThat(operation
                        .path("properties")
                        .path("settledAt")
                        .path("oneOf")
                        .get(1)
                        .path("type")
                        .asString())
                .isEqualTo("null");

        JsonNode exchangeRate = schema("ReceivableResponse").path("properties").path("exchangeRate");
        assertThat(exchangeRate.path("oneOf").get(0).path("$ref").asString())
                .isEqualTo("#/components/schemas/ExchangeRateSnapshot");
        assertThat(texts(schema("PageMetadata").path("required")))
                .containsExactlyInAnyOrder("number", "size", "totalElements", "totalPages");
    }

    @Test
    void requestModelsKeepTheRequirementsOfTheirConstraints() {
        assertThat(texts(schema("ManualExchangeRateRequest").path("required")))
                .containsExactlyInAnyOrder("base", "quote", "rate");
    }

    @Test
    void documentsCreationAndIdempotentReplay() {
        JsonNode create = responses("/api/v1/credit-assignments", "post");
        assertThat(create.propertyNames()).contains("200", "201", "400", "404", "409", "422", "500");
        assertThat(create.path("201").path("headers").has("Location")).isTrue();
        assertThat(create.path("201")
                        .path("content")
                        .path("application/json")
                        .path("schema")
                        .path("$ref")
                        .asString())
                .isEqualTo("#/components/schemas/CreditAssignmentResponse");
    }

    @Test
    void documentsSettlementErrorsAsProblems() {
        JsonNode settle = responses("/api/v1/credit-assignments/{id}/settlement", "post");
        assertThat(settle.propertyNames()).contains("200", "404", "409", "412", "422", "428", "500");
        for (String status : List.of("409", "412", "422", "428")) {
            assertThat(settle.path(status)
                            .path("content")
                            .path("application/problem+json")
                            .path("schema")
                            .path("$ref")
                            .asString())
                    .as("status %s", status)
                    .isEqualTo(PROBLEM_REF);
        }
        assertThat(texts(schema("Problem").path("required"))).contains("code", "correlationId");
        assertThat(responses("/api/v1/exchange-rates/sync", "post").has("503")).isTrue();
    }

    @Test
    void usesExplicitMediaTypes() {
        assertThat(raw).doesNotContain("*/*");
    }
}
