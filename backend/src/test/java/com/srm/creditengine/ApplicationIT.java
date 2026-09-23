package com.srm.creditengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.support.IntegrationTest;
import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.client.RestTestClient;

@IntegrationTest
class ApplicationIT {

    @Autowired
    private Flyway flyway;

    @Autowired
    private RestTestClient client;

    @Test
    void appliesEveryMigrationAndValidatesTheMappingAgainstTheSchema() {
        MigrationInfo[] applied = flyway.info().applied();

        assertThat(applied).isNotEmpty();
        assertThat(Arrays.stream(applied).map(MigrationInfo::getState)).containsOnly(MigrationState.SUCCESS);
        // the context started with ddl-auto=validate, so every entity matches the Flyway schema
    }

    @Test
    void exposesHealthAndOpenApiDocument() {
        client.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("UP");

        client.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.info.title")
                .isEqualTo("SRM Credit Engine API");
    }

    @Test
    void echoesTheCorrelationIdOnEveryResponse() {
        client.get()
                .uri("/actuator/health")
                .header("X-Correlation-Id", "it-correlation-1")
                .exchange()
                .expectHeader()
                .valueEquals("X-Correlation-Id", "it-correlation-1");
    }
}
