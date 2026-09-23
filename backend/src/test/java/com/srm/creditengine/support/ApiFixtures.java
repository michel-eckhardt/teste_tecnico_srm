package com.srm.creditengine.support;

import com.srm.creditengine.web.assignment.CreditAssignmentResponse;
import com.srm.creditengine.web.assignor.AssignorResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/** Builds test data through the public API, as a client would. */
public final class ApiFixtures {

    public static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    private final RestTestClient client;

    public ApiFixtures(RestTestClient client) {
        this.client = client;
    }

    public UUID createAssignor(String name) {
        return client.post()
                .uri("/api/v1/assignors")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"%s\",\"document\":\"%s\"}".formatted(name, Cnpjs.random()))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(AssignorResponse.class)
                .returnResult()
                .getResponseBody()
                .id();
    }

    public void registerRate(String base, String quote, String rate) {
        client.post()
                .uri("/api/v1/exchange-rates")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"base\":\"%s\",\"quote\":\"%s\",\"rate\":\"%s\"}".formatted(base, quote, rate))
                .exchange()
                .expectStatus()
                .isCreated();
    }

    public static String receivable(String type, String faceValue, String faceCurrency, int daysToDue) {
        return "{\"receivableType\":\"%s\",\"faceValue\":\"%s\",\"faceCurrency\":\"%s\",\"dueDate\":\"%s\"}"
                .formatted(type, faceValue, faceCurrency, TODAY.plusDays(daysToDue));
    }

    public static String operation(UUID assignorId, String paymentCurrency, String... receivables) {
        return "{\"assignorId\":\"%s\",\"paymentCurrency\":\"%s\",\"receivables\":[%s]}"
                .formatted(assignorId, paymentCurrency, String.join(",", receivables));
    }

    public RestTestClient.ResponseSpec postOperation(String json, String idempotencyKey) {
        RestTestClient.RequestBodySpec request =
                client.post().uri("/api/v1/credit-assignments").contentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) {
            request = request.header("Idempotency-Key", idempotencyKey);
        }
        return request.body(json).exchange();
    }

    /** Creates a PENDING operation with a single same-currency duplicata. */
    public CreditAssignmentResponse createOperation(UUID assignorId, String currency, String faceValue) {
        return postOperation(
                        operation(assignorId, currency, receivable("DUPLICATA_MERCANTIL", faceValue, currency, 90)),
                        null)
                .expectStatus()
                .isCreated()
                .expectBody(CreditAssignmentResponse.class)
                .returnResult()
                .getResponseBody();
    }
}
