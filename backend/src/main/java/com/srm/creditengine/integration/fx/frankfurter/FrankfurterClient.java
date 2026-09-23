package com.srm.creditengine.integration.fx.frankfurter;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Collection;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client of the Frankfurter API guarded by Resilience4j (instance {@value #RESILIENCE_INSTANCE},
 * configured in application.yml). Retry is the outer decorator, so every attempt is recorded by the
 * circuit breaker; only I/O errors, timeouts and 5xx are retried, 4xx and unreadable payloads are not.
 */
@Component
class FrankfurterClient {

    static final String RESILIENCE_INSTANCE = "frankfurter";

    private final RestClient restClient;

    FrankfurterClient(RestClient frankfurterRestClient) {
        this.restClient = frankfurterRestClient;
    }

    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public FrankfurterLatestResponse latest(String base, Collection<String> symbols) {
        return restClient
                .get()
                .uri(uri -> uri.path("/latest")
                        .queryParam("base", base)
                        .queryParam("symbols", String.join(",", symbols))
                        .build())
                .retrieve()
                .body(FrankfurterLatestResponse.class);
    }
}
