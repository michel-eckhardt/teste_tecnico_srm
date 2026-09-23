package com.srm.creditengine.integration.fx.frankfurter;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.srm.creditengine.domain.currency.CurrencyCode.BRL;
import static com.srm.creditengine.domain.currency.CurrencyCode.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.srm.creditengine.domain.currency.ExchangeRateProvider;
import com.srm.creditengine.domain.currency.FxProviderUnavailableException;
import com.srm.creditengine.domain.currency.ProvidedRate;
import com.srm.creditengine.support.IntegrationTest;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@IntegrationTest
class FrankfurterExchangeRateProviderIT {

    private static final String LATEST = "/v1/latest";
    private static final String BODY =
            "{\"amount\":1.0,\"base\":\"USD\",\"date\":\"2026-09-23\",\"rates\":{\"BRL\":5.1322}}";

    @Autowired
    private ExchangeRateProvider provider;

    @Autowired
    private WireMockServer frankfurter;

    @Autowired
    private CircuitBreakerRegistry circuitBreakers;

    private CircuitBreaker breaker;

    @BeforeEach
    void reset() {
        frankfurter.resetAll();
        breaker = circuitBreakers.circuitBreaker(FrankfurterClient.RESILIENCE_INSTANCE);
        breaker.reset();
    }

    private void respondWith(ResponseDefinitionBuilder response) {
        frankfurter.stubFor(get(urlPathEqualTo(LATEST)).willReturn(response));
    }

    private List<ProvidedRate> fetch() {
        return provider.fetchLatest(USD, Set.of(BRL));
    }

    private int requests() {
        return frankfurter.findAll(getRequestedFor(urlPathEqualTo(LATEST))).size();
    }

    @Test
    void mapsTheLatestRatesWithoutFloatingPointConversion() {
        frankfurter.stubFor(get(urlPathEqualTo(LATEST))
                .withQueryParam("base", equalTo("USD"))
                .withQueryParam("symbols", equalTo("BRL"))
                .willReturn(okJson(BODY)));

        List<ProvidedRate> rates = fetch();

        assertThat(rates)
                .containsExactly(new ProvidedRate(USD, BRL, new BigDecimal("5.1322"), LocalDate.of(2026, 9, 23)));
        assertThat(requests()).isEqualTo(1);
    }

    @Test
    void retriesServerErrorsThenReportsTheProviderAsUnavailable() {
        respondWith(serverError());

        assertThatExceptionOfType(FxProviderUnavailableException.class)
                .isThrownBy(this::fetch)
                .withCauseInstanceOf(HttpServerErrorException.class);
        assertThat(requests()).isEqualTo(3);
    }

    @Test
    void retriesTimeouts() {
        respondWith(okJson(BODY).withFixedDelay(1_500));

        assertThatExceptionOfType(FxProviderUnavailableException.class)
                .isThrownBy(this::fetch)
                .withCauseInstanceOf(ResourceAccessException.class);
        assertThat(requests()).isEqualTo(3);
    }

    @Test
    void doesNotRetryClientErrorsNorCountThemAgainstTheProviderHealth() {
        respondWith(notFound());

        assertThatExceptionOfType(FxProviderUnavailableException.class)
                .isThrownBy(this::fetch)
                .withCauseInstanceOf(HttpClientErrorException.class);
        assertThat(requests()).isEqualTo(1);
        assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    void doesNotRetryMalformedJson() {
        respondWith(okJson("{\"base\":\"USD\",\"rates\":"));

        assertThatExceptionOfType(FxProviderUnavailableException.class)
                .isThrownBy(this::fetch)
                .withCauseInstanceOf(RestClientException.class);
        assertThat(requests()).isEqualTo(1);
    }

    @Test
    void rejectsPayloadsWithoutTheRequestedQuote() {
        respondWith(okJson("{\"amount\":1.0,\"base\":\"USD\",\"date\":\"2026-09-23\",\"rates\":{\"EUR\":0.9}}"));

        assertThatExceptionOfType(FxProviderUnavailableException.class)
                .isThrownBy(this::fetch)
                .withMessageContaining("BRL");
    }

    @Test
    void opensTheCircuitAfterRepeatedFailuresAndStopsCallingTheProvider() {
        respondWith(serverError());

        // 1st sync: 3 failed attempts. 2nd sync: 2 more failures reach the minimum of 5 calls with a
        // 100% failure rate, the breaker opens and the 3rd attempt is short-circuited.
        assertThatExceptionOfType(FxProviderUnavailableException.class).isThrownBy(this::fetch);
        assertThatExceptionOfType(FxProviderUnavailableException.class)
                .isThrownBy(this::fetch)
                .withCauseInstanceOf(CallNotPermittedException.class);
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(requests()).isEqualTo(5);

        assertThatExceptionOfType(FxProviderUnavailableException.class)
                .isThrownBy(this::fetch)
                .withMessageContaining("circuit breaker");
        assertThat(requests()).isEqualTo(5);
    }
}
