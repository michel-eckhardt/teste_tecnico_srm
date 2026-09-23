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
import org.springframework.test.web.servlet.client.RestTestClient;

@IntegrationTest
class SettlementApiIT {

    @Autowired
    private RestTestClient client;

    @Autowired
    private JdbcClient jdbc;

    private ApiFixtures fixtures;
    private UUID assignorId;

    @BeforeEach
    void setUp() {
        fixtures = new ApiFixtures(client);
        assignorId = fixtures.createAssignor("Cedente Liquidação Ltda");
    }

    private RestTestClient.ResponseSpec settle(UUID id, String ifMatch) {
        RestTestClient.RequestBodySpec request = client.post().uri("/api/v1/credit-assignments/{id}/settlement", id);
        if (ifMatch != null) {
            request = request.header("If-Match", ifMatch);
        }
        return request.exchange();
    }

    private RestTestClient.ResponseSpec cancel(UUID id, String ifMatch) {
        return client.post()
                .uri("/api/v1/credit-assignments/{id}/cancellation", id)
                .header("If-Match", ifMatch)
                .exchange();
    }

    private BigDecimal balance(String currency) {
        return jdbc.sql("SELECT balance FROM fund_cash_account WHERE currency = :currency")
                .param("currency", currency)
                .query(BigDecimal.class)
                .single();
    }

    private long debitsOf(UUID operationId) {
        return jdbc.sql("SELECT count(*) FROM cash_movement WHERE credit_assignment_id = :id AND direction = 'DEBIT'")
                .param("id", operationId)
                .query(Long.class)
                .single();
    }

    private String statusOf(UUID operationId) {
        return jdbc.sql("SELECT status FROM credit_assignment WHERE id = :id")
                .param("id", operationId)
                .query(String.class)
                .single();
    }

    @Test
    void settlementDebitsTheFundAndRecordsTheMovementAtomically() {
        CreditAssignmentResponse created = fixtures.createOperation(assignorId, "BRL", "10000.00");
        BigDecimal before = balance("BRL");

        CreditAssignmentResponse settled = settle(created.id(), "\"0\"")
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueEquals("ETag", "\"1\"")
                .expectBody(CreditAssignmentResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(settled.status()).isEqualTo(CreditAssignmentStatus.SETTLED);
        assertThat(settled.settledAt()).isNotNull();
        assertThat(settled.version()).isEqualTo(1);
        assertThat(balance("BRL")).isEqualTo(before.subtract(new BigDecimal("9285.99")));
        assertThat(debitsOf(created.id())).isEqualTo(1);
        BigDecimal movement = jdbc.sql("SELECT amount FROM cash_movement WHERE credit_assignment_id = :id")
                .param("id", created.id())
                .query(BigDecimal.class)
                .single();
        assertThat(movement).isEqualTo(new BigDecimal("9285.99"));

        client.get()
                .uri("/api/v1/cash-accounts")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[?(@.currency == 'BRL')].balance")
                .isEqualTo(java.util.List.of(balance("BRL").toPlainString()));
    }

    @Test
    void aSecondSettlementIsAConflict() {
        CreditAssignmentResponse created = fixtures.createOperation(assignorId, "BRL", "1000.00");
        settle(created.id(), "\"0\"").expectStatus().isOk();
        BigDecimal afterFirst = balance("BRL");

        settle(created.id(), "\"1\"")
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("OPERATION_ALREADY_SETTLED");
        assertThat(balance("BRL")).isEqualTo(afterFirst);
        assertThat(debitsOf(created.id())).isEqualTo(1);
    }

    @Test
    void aStaleIfMatchIsRejectedWith412() {
        CreditAssignmentResponse created = fixtures.createOperation(assignorId, "BRL", "1000.00");
        settle(created.id(), "\"0\"").expectStatus().isOk();

        settle(created.id(), "\"0\"")
                .expectStatus()
                .isEqualTo(412)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("PRECONDITION_FAILED");
    }

    @Test
    void theIfMatchHeaderIsRequiredAndMustCarryAVersion() {
        CreditAssignmentResponse created = fixtures.createOperation(assignorId, "BRL", "1000.00");

        settle(created.id(), null)
                .expectStatus()
                .isEqualTo(428)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("PRECONDITION_REQUIRED");
        settle(created.id(), "*")
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0].field")
                .isEqualTo("If-Match");
        assertThat(statusOf(created.id())).isEqualTo("PENDING");
    }

    @Test
    void insufficientFundsLeavesNothingBehind() {
        CreditAssignmentResponse created = fixtures.postOperation(
                        operation(assignorId, "USD", receivable("DUPLICATA_MERCANTIL", "1000000000.00", "USD", 30)),
                        null)
                .expectStatus()
                .isCreated()
                .expectBody(CreditAssignmentResponse.class)
                .returnResult()
                .getResponseBody();
        BigDecimal before = balance("USD");

        settle(created.id(), "\"0\"")
                .expectStatus()
                .isEqualTo(422)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("INSUFFICIENT_FUNDS");

        assertThat(balance("USD")).isEqualTo(before);
        assertThat(statusOf(created.id())).isEqualTo("PENDING");
        assertThat(debitsOf(created.id())).isZero();
        client.get()
                .uri("/api/v1/credit-assignments/{id}", created.id())
                .exchange()
                .expectHeader()
                .valueEquals("ETag", "\"0\"");
    }

    @Test
    void cancelsAPendingOperationAndThenRefusesToSettleIt() {
        CreditAssignmentResponse created = fixtures.createOperation(assignorId, "BRL", "1000.00");

        cancel(created.id(), "\"0\"")
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueEquals("ETag", "\"1\"")
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("CANCELLED")
                .jsonPath("$.cancelledAt")
                .isNotEmpty();

        settle(created.id(), "\"1\"")
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("INVALID_STATE_TRANSITION");
        assertThat(debitsOf(created.id())).isZero();
    }

    @Test
    void aSettledOperationCannotBeCancelled() {
        CreditAssignmentResponse created = fixtures.createOperation(assignorId, "BRL", "1000.00");
        settle(created.id(), "\"0\"").expectStatus().isOk();

        cancel(created.id(), "\"1\"")
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("INVALID_STATE_TRANSITION");
    }

    @Test
    void settlingAnUnknownOperationIsNotFound() {
        settle(UUID.randomUUID(), "\"0\"").expectStatus().isNotFound();
    }
}
