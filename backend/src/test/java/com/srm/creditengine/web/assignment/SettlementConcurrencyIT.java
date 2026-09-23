package com.srm.creditengine.web.assignment;

import static com.srm.creditengine.support.ApiFixtures.operation;
import static com.srm.creditengine.support.ApiFixtures.receivable;
import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.support.ApiFixtures;
import com.srm.creditengine.support.IntegrationTest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Race conditions of the settlement, reproduced with real concurrent HTTP requests released at the
 * same instant by a latch.
 */
@IntegrationTest
class SettlementConcurrencyIT {

    private static final int THREADS = 8;

    @Autowired
    private RestTestClient client;

    @Autowired
    private JdbcClient jdbc;

    private ApiFixtures fixtures;
    private UUID assignorId;

    @BeforeEach
    void setUp() {
        fixtures = new ApiFixtures(client);
        assignorId = fixtures.createAssignor("Cedente Concorrência Ltda");
    }

    /** Runs every task on its own thread, all released together, and returns their results. */
    private static <T> List<T> runConcurrently(List<Callable<T>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.call();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private int settle(UUID operationId, String ifMatch) {
        return client.post()
                .uri("/api/v1/credit-assignments/{id}/settlement", operationId)
                .header("If-Match", ifMatch)
                .exchange()
                .returnResult()
                .getStatus()
                .value();
    }

    private BigDecimal balance(String currency) {
        return jdbc.sql("SELECT balance FROM fund_cash_account WHERE currency = :currency")
                .param("currency", currency)
                .query(BigDecimal.class)
                .single();
    }

    private long debitsOf(List<UUID> operationIds) {
        return jdbc.sql(
                        "SELECT count(*) FROM cash_movement WHERE direction = 'DEBIT' AND credit_assignment_id IN (:ids)")
                .param("ids", operationIds)
                .query(Long.class)
                .single();
    }

    @Test
    void theSameOperationIsPaidExactlyOnceWhenSettledConcurrently() throws Exception {
        CreditAssignmentResponse created = fixtures.createOperation(assignorId, "BRL", "1000.00");
        BigDecimal before = balance("BRL");

        List<Callable<Integer>> attempts = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            attempts.add(() -> settle(created.id(), "\"0\""));
        }
        List<Integer> statuses = runConcurrently(attempts);

        assertThat(statuses).filteredOn(status -> status == 200).hasSize(1);
        assertThat(statuses).filteredOn(status -> status != 200).allMatch(status -> status == 409 || status == 412);
        assertThat(debitsOf(List.of(created.id()))).isEqualTo(1);
        assertThat(balance("BRL")).isEqualTo(before.subtract(created.totalNetAmount()));
    }

    @Test
    void differentOperationsInTheSameCurrencyAreAllSettledWithoutLostUpdates() throws Exception {
        List<CreditAssignmentResponse> operations = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            operations.add(fixtures.createOperation(assignorId, "BRL", "%d.00".formatted(1000 + i)));
        }
        BigDecimal before = balance("BRL");
        BigDecimal expectedDebit = operations.stream()
                .map(CreditAssignmentResponse::totalNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Callable<Integer>> settlements = operations.stream()
                .<Callable<Integer>>map(operation -> () -> settle(operation.id(), "\"0\""))
                .toList();
        List<Integer> statuses = runConcurrently(settlements);

        assertThat(statuses).containsOnly(200);
        assertThat(balance("BRL")).isEqualTo(before.subtract(expectedDebit));
        assertThat(debitsOf(
                        operations.stream().map(CreditAssignmentResponse::id).toList()))
                .isEqualTo(THREADS);
    }

    @Test
    void identicalCreationRequestsRacingWithTheSameKeyCreateASingleOperation() throws Exception {
        String key = UUID.randomUUID().toString();
        String json = operation(assignorId, "BRL", receivable("CHEQUE_PRE_DATADO", "777.77", "BRL", 60));

        List<Callable<EntityExchangeResult<CreditAssignmentResponse>>> requests = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            requests.add(() -> fixtures.postOperation(json, key)
                    .expectBody(CreditAssignmentResponse.class)
                    .returnResult());
        }
        List<EntityExchangeResult<CreditAssignmentResponse>> results = runConcurrently(requests);

        assertThat(results)
                .extracting(result -> result.getStatus().value())
                .containsOnly(200, 201)
                .containsOnlyOnce(201);
        assertThat(results)
                .extracting(result -> result.getResponseBody().id())
                .containsOnly(results.getFirst().getResponseBody().id());
        long stored = jdbc.sql("SELECT count(*) FROM credit_assignment WHERE idempotency_key = :key")
                .param("key", key)
                .query(Long.class)
                .single();
        assertThat(stored).isEqualTo(1);
    }
}
