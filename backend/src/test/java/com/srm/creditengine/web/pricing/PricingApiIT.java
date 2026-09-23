package com.srm.creditengine.web.pricing;

import com.srm.creditengine.support.IntegrationTest;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@IntegrationTest
class PricingApiIT {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    @Autowired
    private RestTestClient client;

    private RestTestClient.ResponseSpec simulate(
            String type, String face, String faceCurrency, LocalDate due, String pay) {
        return client.post()
                .uri("/api/v1/pricing/simulations")
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        "{\"receivableType\":\"%s\",\"faceValue\":\"%s\",\"faceCurrency\":\"%s\",\"dueDate\":\"%s\",\"paymentCurrency\":\"%s\"}"
                                .formatted(type, face, faceCurrency, due, pay))
                .exchange();
    }

    @Test
    void listsTheSeededReceivableTypesWithTheirStrategySpread() {
        client.get()
                .uri("/api/v1/receivable-types")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[?(@.code == 'DUPLICATA_MERCANTIL')].monthlySpread")
                .isEqualTo(java.util.List.of("0.01500000"))
                .jsonPath("$[?(@.code == 'CHEQUE_PRE_DATADO')].monthlySpread")
                .isEqualTo(java.util.List.of("0.02500000"))
                .jsonPath("$[?(@.code == 'CHEQUE_PRE_DATADO')].description")
                .isEqualTo(java.util.List.of("Cheque Pré-datado"));
    }

    @Test
    void simulatesASameCurrencyReceivable() {
        simulate("DUPLICATA_MERCANTIL", "10000.00", "BRL", TODAY.plusDays(90), "BRL")
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.operationDate")
                .isEqualTo(TODAY.toString())
                .jsonPath("$.termDays")
                .isEqualTo(90)
                .jsonPath("$.presentValue")
                .isEqualTo("9285.99")
                .jsonPath("$.discount")
                .isEqualTo("714.01")
                .jsonPath("$.netAmount")
                .isEqualTo("9285.99")
                .jsonPath("$.exchangeRate")
                .isEmpty();
    }

    @Test
    void simulatesACrossCurrencyReceivableWithTheLatestPersistedRate() {
        client.post()
                .uri("/api/v1/exchange-rates")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"base\":\"USD\",\"quote\":\"BRL\",\"rate\":\"5.1322\"}")
                .exchange()
                .expectStatus()
                .isCreated();

        simulate("DUPLICATA_MERCANTIL", "10000.00", "BRL", TODAY.plusDays(90), "USD")
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.presentValue")
                .isEqualTo("9285.99")
                .jsonPath("$.exchangeRate.base")
                .isEqualTo("USD")
                .jsonPath("$.exchangeRate.rate")
                .isEqualTo("5.13220000")
                .jsonPath("$.netAmount")
                .isEqualTo("1809.36");
    }

    @Test
    void rejectsUnknownReceivableTypes() {
        simulate("NOTA_PROMISSORIA", "100.00", "BRL", TODAY.plusDays(30), "BRL")
                .expectStatus()
                .isEqualTo(422)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("UNSUPPORTED_RECEIVABLE_TYPE");
    }

    @Test
    void rejectsADueDateThatIsNotInTheFuture() {
        simulate("CHEQUE_PRE_DATADO", "100.00", "BRL", TODAY, "BRL")
                .expectStatus()
                .isEqualTo(422)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("INVALID_DUE_DATE");
    }
}
