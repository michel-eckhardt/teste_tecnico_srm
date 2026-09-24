package com.srm.creditengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.client.RestTestClient;

@IntegrationTest
class ActuatorIT {

    @Autowired
    private RestTestClient client;

    private String get(String path) {
        return client.get()
                .uri(path)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
    }

    @Test
    void livenessDoesNotDependOnExternalSystems() {
        client.get()
                .uri("/actuator/health/liveness")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("UP")
                .jsonPath("$.components.db")
                .doesNotExist();
    }

    @Test
    void readinessRequiresTheDatabaseButNotTheFxProvider() {
        client.get()
                .uri("/actuator/health/readiness")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("UP")
                .jsonPath("$.components.db.status")
                .isEqualTo("UP")
                .jsonPath("$.components.circuitBreakers")
                .doesNotExist();
    }

    @Test
    void exposesPrometheusMetricsWithTheApplicationTag() {
        String metrics = get("/actuator/prometheus");

        assertThat(metrics)
                .contains("application=\"srm-credit-engine\"")
                .contains("resilience4j_circuitbreaker_state")
                .contains("http_server_requests_seconds");
    }

    @Test
    void publishesHttpLatencyHistogramsForPercentileDashboards() {
        get("/api/v1/currencies");

        assertThat(get("/actuator/prometheus")).contains("http_server_requests_seconds_bucket{");
    }

    @Test
    void exposesBuildInfoAndCircuitBreakers() {
        assertThat(get("/actuator/info")).contains("\"build\"").contains("credit-engine");
        assertThat(get("/actuator/circuitbreakers")).contains("frankfurter");
    }
}
