package com.srm.creditengine.web.currency;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.support.IntegrationTest;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@IntegrationTest
class ExchangeRateApiIT {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    @Autowired
    private RestTestClient client;

    private URI registerManual(String json) {
        return client.post()
                .uri("/api/v1/exchange-rates")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .returnResult()
                .getResponseHeaders()
                .getLocation();
    }

    @Test
    void listsTheSeededCurrencies() {
        client.get()
                .uri("/api/v1/currencies")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[*].code")
                .isEqualTo(java.util.List.of("BRL", "USD"));
    }

    @Test
    void manualRateIsRetrievableAndBecomesTheLatestInBothDirections() {
        URI location = registerManual("{\"base\":\"BRL\",\"quote\":\"USD\",\"rate\":\"0.19\"}");

        client.get()
                .uri(location)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.rate")
                .isEqualTo("0.19000000")
                .jsonPath("$.source")
                .isEqualTo("MANUAL")
                .jsonPath("$.referenceDate")
                .isEqualTo(TODAY.toString());

        String id = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
        client.get()
                .uri("/api/v1/exchange-rates/latest?base=USD&quote=BRL")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(id)
                .jsonPath("$.base")
                .isEqualTo("USD")
                .jsonPath("$.rate")
                .isEqualTo("5.26315789")
                .jsonPath("$.derived")
                .isEqualTo(true)
                .jsonPath("$.stale")
                .isEqualTo(false);
    }

    @Test
    void historyListsThePublishedPairNewestFirst() {
        registerManual("{\"base\":\"USD\",\"quote\":\"BRL\",\"rate\":\"5.10\",\"referenceDate\":\""
                + TODAY.minusDays(10) + "\"}");
        registerManual("{\"base\":\"USD\",\"quote\":\"BRL\",\"rate\":\"5.30\"}");

        client.get()
                .uri("/api/v1/exchange-rates?base=USD&quote=BRL&page=0&size=100")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.content[0].rate")
                .isEqualTo("5.30000000")
                .jsonPath("$.content[?(@.rate == '5.10000000')].stale")
                .isEqualTo(java.util.List.of(true))
                .jsonPath("$.page.size")
                .isEqualTo(100)
                .jsonPath("$.page.totalElements")
                .value(total -> assertThat(((Number) total).longValue()).isGreaterThanOrEqualTo(2));
    }

    @Test
    void unknownRateIdIsNotFound() {
        client.get()
                .uri("/api/v1/exchange-rates/01996f3c-0000-7000-8000-000000000000")
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }
}
