package com.srm.creditengine.web.assignment;

import static com.srm.creditengine.support.ApiFixtures.operation;
import static com.srm.creditengine.support.ApiFixtures.receivable;
import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.domain.assignment.CreditAssignmentStatus;
import com.srm.creditengine.support.ApiFixtures;
import com.srm.creditengine.support.IntegrationTest;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

@IntegrationTest
class CreditAssignmentApiIT {

    @Autowired
    private RestTestClient client;

    @Autowired
    private JdbcClient jdbc;

    private ApiFixtures fixtures;
    private UUID assignorId;

    @BeforeEach
    void setUp() {
        fixtures = new ApiFixtures(client);
        assignorId = fixtures.createAssignor("Cedente Operações Ltda");
    }

    private long operationsOf(UUID assignor) {
        return jdbc.sql("SELECT count(*) FROM credit_assignment WHERE assignor_id = :id")
                .param("id", assignor)
                .query(Long.class)
                .single();
    }

    @Test
    void createsAPricedPendingOperationWithEtagAndLocation() {
        String json = operation(
                assignorId,
                "BRL",
                receivable("DUPLICATA_MERCANTIL", "10000.00", "BRL", 90),
                receivable("CHEQUE_PRE_DATADO", "10000.00", "BRL", 45));

        EntityExchangeResult<CreditAssignmentResponse> created = fixtures.postOperation(json, null)
                .expectStatus()
                .isCreated()
                .expectHeader()
                .valueEquals("ETag", "\"0\"")
                .expectBody(CreditAssignmentResponse.class)
                .returnResult();
        CreditAssignmentResponse body = created.getResponseBody();

        assertThat(created.getResponseHeaders().getLocation()).hasPath("/api/v1/credit-assignments/" + body.id());
        assertThat(body.status()).isEqualTo(CreditAssignmentStatus.PENDING);
        assertThat(body.version()).isZero();
        assertThat(body.assignor().id()).isEqualTo(assignorId);
        assertThat(body.receivablesCount()).isEqualTo(2);
        // 9285.99 (duplicata, 90 days) + 9497.07 (cheque, 45 days)
        assertThat(body.totalNetAmount()).isEqualTo(new BigDecimal("18783.06"));
        assertThat(body.totalFaceValue()).isEqualTo(new BigDecimal("20000.00"));
        assertThat(body.totalDiscount()).isEqualTo(new BigDecimal("1216.94"));
        assertThat(body.receivables())
                .extracting(CreditAssignmentResponse.ReceivableResponse::presentValue)
                .containsExactly(new BigDecimal("9285.99"), new BigDecimal("9497.07"));
        assertThat(body.receivables())
                .allSatisfy(item -> assertThat(item.exchangeRate()).isNull());

        client.get()
                .uri(created.getResponseHeaders().getLocation())
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueEquals("ETag", "\"0\"")
                .expectBody()
                .jsonPath("$.totalNetAmount")
                .isEqualTo("18783.06")
                .jsonPath("$.settledAt")
                .isEmpty();
    }

    @Test
    void keepsTheFxSnapshotOfCrossCurrencyReceivables() {
        fixtures.registerRate("USD", "BRL", "5.1322");

        CreditAssignmentResponse body = fixtures.postOperation(
                        operation(assignorId, "BRL", receivable("CHEQUE_PRE_DATADO", "2500.00", "USD", 48)), null)
                .expectStatus()
                .isCreated()
                .expectBody(CreditAssignmentResponse.class)
                .returnResult()
                .getResponseBody();

        CreditAssignmentResponse.ReceivableResponse item = body.receivables().getFirst();
        assertThat(item.presentValue()).isEqualTo(new BigDecimal("2384.52"));
        assertThat(item.netAmount()).isEqualTo(new BigDecimal("12237.82"));
        assertThat(item.exchangeRate().base().name()).isEqualTo("USD");
        assertThat(item.exchangeRate().rate()).isEqualTo(new BigDecimal("5.13220000"));
        assertThat(body.totalFaceValue()).isEqualTo(new BigDecimal("12830.50"));
    }

    @Test
    void replaysTheSameOperationForARepeatedIdempotencyKey() {
        String key = UUID.randomUUID().toString();
        String json = operation(assignorId, "BRL", receivable("DUPLICATA_MERCANTIL", "5000.00", "BRL", 30));

        UUID first = fixtures.postOperation(json, key)
                .expectStatus()
                .isCreated()
                .expectBody(CreditAssignmentResponse.class)
                .returnResult()
                .getResponseBody()
                .id();
        UUID replayed = fixtures.postOperation(json.replace("5000.00", "5000"), key)
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueEquals("ETag", "\"0\"")
                .expectBody(CreditAssignmentResponse.class)
                .returnResult()
                .getResponseBody()
                .id();

        assertThat(replayed).isEqualTo(first);
        assertThat(operationsOf(assignorId)).isEqualTo(1);
    }

    @Test
    void rejectsAnIdempotencyKeyReusedWithAnotherPayload() {
        String key = UUID.randomUUID().toString();
        fixtures.postOperation(
                        operation(assignorId, "BRL", receivable("DUPLICATA_MERCANTIL", "100.00", "BRL", 30)), key)
                .expectStatus()
                .isCreated();

        fixtures.postOperation(
                        operation(assignorId, "BRL", receivable("DUPLICATA_MERCANTIL", "200.00", "BRL", 30)), key)
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("IDEMPOTENCY_KEY_REUSED");
        assertThat(operationsOf(assignorId)).isEqualTo(1);
    }

    @Test
    void unknownAssignorIsNotFound() {
        fixtures.postOperation(
                        operation(UUID.randomUUID(), "BRL", receivable("DUPLICATA_MERCANTIL", "100.00", "BRL", 30)),
                        null)
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void anInvalidReceivableRejectsTheWholeBatch() {
        fixtures.postOperation(
                        operation(
                                assignorId,
                                "BRL",
                                receivable("DUPLICATA_MERCANTIL", "100.00", "BRL", 30),
                                receivable("NOTA_PROMISSORIA", "100.00", "BRL", 30)),
                        null)
                .expectStatus()
                .isEqualTo(422)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("UNSUPPORTED_RECEIVABLE_TYPE");
        assertThat(operationsOf(assignorId)).isZero();
    }

    @Test
    void validatesTheBatchAndTheIdempotencyKey() {
        fixtures.postOperation(operation(assignorId, "BRL"), null)
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0].field")
                .isEqualTo("receivables");

        fixtures.postOperation(
                        operation(assignorId, "BRL", receivable("DUPLICATA_MERCANTIL", "100.00", "BRL", 30)),
                        "not a valid key!")
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void unknownOperationIsNotFound() {
        client.get()
                .uri("/api/v1/credit-assignments/{id}", UUID.randomUUID())
                .exchange()
                .expectStatus()
                .isNotFound();
    }
}
