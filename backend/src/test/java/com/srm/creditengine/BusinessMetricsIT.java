package com.srm.creditengine;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.srm.creditengine.support.ApiFixtures;
import com.srm.creditengine.support.IntegrationTest;
import com.srm.creditengine.web.assignment.CreditAssignmentResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.client.RestTestClient;

/** Business meters observed through the same registry that backs /actuator/prometheus. */
@IntegrationTest
class BusinessMetricsIT {

    @Autowired
    private RestTestClient client;

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private WireMockServer frankfurter;

    @Autowired
    private CircuitBreakerRegistry circuitBreakers;

    private double count(String name, String... tags) {
        var counter = registry.find(name).tags(tags).counter();
        return counter == null ? 0 : counter.count();
    }

    private long timerCount(String name, String... tags) {
        var timer = registry.find(name).tags(tags).timer();
        return timer == null ? 0 : timer.count();
    }

    @Test
    void countsCreatedSettledAndConflictingOperations() {
        ApiFixtures fixtures = new ApiFixtures(client);
        UUID assignor = fixtures.createAssignor("Cedente Métricas Ltda");
        double createdBefore = count("srm.credit_assignments.opened", "payment_currency", "BRL");
        double settledBefore = count("srm.credit_assignments.settled", "payment_currency", "BRL");
        double conflictsBefore = count("srm.credit_assignments.settlement.conflicts", "reason", "stale_version");
        long pricingBefore = timerCount("srm.pricing.duration", "payment_currency", "BRL");

        CreditAssignmentResponse operation = fixtures.createOperation(assignor, "BRL", "1500.00");
        client.post()
                .uri("/api/v1/credit-assignments/{id}/settlement", operation.id())
                .header("If-Match", "\"0\"")
                .exchange()
                .expectStatus()
                .isOk();
        client.post()
                .uri("/api/v1/credit-assignments/{id}/settlement", operation.id())
                .header("If-Match", "\"0\"")
                .exchange()
                .expectStatus()
                .isEqualTo(412);

        assertThat(count("srm.credit_assignments.opened", "payment_currency", "BRL"))
                .isEqualTo(createdBefore + 1);
        assertThat(count("srm.credit_assignments.settled", "payment_currency", "BRL"))
                .isEqualTo(settledBefore + 1);
        assertThat(count("srm.credit_assignments.settlement.conflicts", "reason", "stale_version"))
                .isEqualTo(conflictsBefore + 1);
        assertThat(timerCount("srm.pricing.duration", "payment_currency", "BRL"))
                .isEqualTo(pricingBefore + 1);

        String prometheus = client.get()
                .uri("/actuator/prometheus")
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        assertThat(prometheus)
                .contains("srm_credit_assignments_opened_total")
                .contains("srm_credit_assignments_settled_total")
                .contains("srm_pricing_duration_seconds_bucket");
    }

    @Test
    void timesProviderCallsByOutcome() {
        frankfurter.resetAll();
        circuitBreakers.circuitBreaker("frankfurter").reset();
        frankfurter.stubFor(get(urlPathEqualTo("/v1/latest")).willReturn(serverError()));
        long failuresBefore = timerCount("srm.fx.provider.calls", "provider", "frankfurter", "outcome", "failure");

        client.post()
                .uri("/api/v1/exchange-rates/sync")
                .exchange()
                .expectStatus()
                .isEqualTo(503);

        assertThat(timerCount("srm.fx.provider.calls", "provider", "frankfurter", "outcome", "failure"))
                .isEqualTo(failuresBefore + 1);
        circuitBreakers.circuitBreaker("frankfurter").reset();
    }
}
