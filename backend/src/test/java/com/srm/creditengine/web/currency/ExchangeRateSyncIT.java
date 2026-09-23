package com.srm.creditengine.web.currency;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.srm.creditengine.support.IntegrationTest;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.RestTestClient;

@IntegrationTest
class ExchangeRateSyncIT {

    /** A past ECB date, so the synchronized rate never becomes "the latest" for other tests. */
    private static final String ECB_PAYLOAD =
            "{\"amount\":1.0,\"base\":\"USD\",\"date\":\"2026-09-18\",\"rates\":{\"BRL\":5.4321}}";

    @Autowired
    private RestTestClient client;

    @Autowired
    private WireMockServer frankfurter;

    @Autowired
    private CircuitBreakerRegistry circuitBreakers;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void reset() {
        frankfurter.resetAll();
        circuitBreakers.circuitBreaker("frankfurter").reset();
    }

    private long storedFrankfurterRates() {
        return jdbc.sql("SELECT count(*) FROM exchange_rate WHERE source = 'FRANKFURTER' AND rate = 5.4321")
                .query(Long.class)
                .single();
    }

    @Test
    void persistsTheProviderRateOnceEvenWhenSynchronizedTwice() {
        frankfurter.stubFor(get(urlPathEqualTo("/v1/latest")).willReturn(okJson(ECB_PAYLOAD)));

        for (int attempt = 0; attempt < 2; attempt++) {
            client.post()
                    .uri("/api/v1/exchange-rates/sync")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$[0].base")
                    .isEqualTo("USD")
                    .jsonPath("$[0].quote")
                    .isEqualTo("BRL")
                    .jsonPath("$[0].rate")
                    .isEqualTo("5.43210000")
                    .jsonPath("$[0].source")
                    .isEqualTo("FRANKFURTER")
                    .jsonPath("$[0].referenceDate")
                    .isEqualTo("2026-09-18");
        }

        assertThat(storedFrankfurterRates()).isEqualTo(1);
    }

    @Test
    void answers503WhenTheProviderIsDown() {
        frankfurter.stubFor(get(urlPathEqualTo("/v1/latest")).willReturn(serviceUnavailable()));

        client.post()
                .uri("/api/v1/exchange-rates/sync")
                .exchange()
                .expectStatus()
                .isEqualTo(503)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("FX_PROVIDER_UNAVAILABLE")
                .jsonPath("$.title")
                .isEqualTo("Provedor de câmbio indisponível");
    }
}
