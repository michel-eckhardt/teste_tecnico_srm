package com.srm.creditengine.web.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.support.Cnpjs;
import com.srm.creditengine.support.IntegrationTest;
import com.srm.creditengine.web.support.PageResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * The statement is checked against rows inserted with controlled instants (business zone
 * America/Sao_Paulo, UTC-3), each test scoped to its own assignor so data from other tests never
 * interferes.
 */
@IntegrationTest
class SettlementStatementIT {

    private static final ParameterizedTypeReference<PageResponse<SettlementStatementItem>> PAGE =
            new ParameterizedTypeReference<>() {};

    @Autowired
    private RestTestClient client;

    @Autowired
    private JdbcClient jdbc;

    private UUID assignorId;
    private UUID jan9Settled;
    private UUID jan9LastSecondPending;
    private UUID jan10MidnightCancelled;
    private UUID jan11Settled;

    @BeforeEach
    void seed() {
        assignorId = UUID.randomUUID();
        jdbc.sql("INSERT INTO assignor (id, name, document, created_at) VALUES (:id, :name, :document, now())")
                .param("id", assignorId)
                .param("name", "Relatório " + assignorId)
                .param("document", Cnpjs.random())
                .update();
        // 12:00 UTC = 09:00 on Jan 9 in Sao Paulo
        jan9Settled = insert("2026-01-09T12:00:00Z", "BRL", "SETTLED", "100.00");
        // 02:59:59 UTC on Jan 10 = 23:59:59 on Jan 9 in Sao Paulo: still Jan 9
        jan9LastSecondPending = insert("2026-01-10T02:59:59Z", "USD", "PENDING", "200.00");
        // 03:00 UTC on Jan 10 = midnight of Jan 10 in Sao Paulo
        jan10MidnightCancelled = insert("2026-01-10T03:00:00Z", "BRL", "CANCELLED", "300.00");
        jan11Settled = insert("2026-01-11T15:00:00Z", "BRL", "SETTLED", "50.00");
    }

    private UUID insert(String createdAt, String currency, String status, String net) {
        UUID id = UUID.randomUUID();
        OffsetDateTime created = Instant.parse(createdAt).atOffset(ZoneOffset.UTC);
        BigDecimal netAmount = new BigDecimal(net);
        jdbc.sql("""
                        INSERT INTO credit_assignment (id, assignor_id, payment_currency, status, total_face_value,
                                                       total_discount, total_net_amount, receivables_count, version,
                                                       created_at, settled_at, cancelled_at)
                        VALUES (:id, :assignor, :currency, :status, :face, :discount, :net, 1, 0,
                                :created, :settled, :cancelled)
                        """)
                .param("id", id)
                .param("assignor", assignorId)
                .param("currency", currency)
                .param("status", status)
                .param("face", netAmount.add(BigDecimal.TEN))
                .param("discount", BigDecimal.TEN.setScale(2))
                .param("net", netAmount)
                .param("created", created)
                .param("settled", status.equals("SETTLED") ? created.plusHours(1) : null)
                .param("cancelled", status.equals("CANCELLED") ? created.plusHours(2) : null)
                .update();
        return id;
    }

    private PageResponse<SettlementStatementItem> statement(String query) {
        return client.get()
                .uri("/api/v1/reports/settlement-statement?assignorId=" + assignorId + query)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE)
                .returnResult()
                .getResponseBody();
    }

    private static List<UUID> ids(PageResponse<SettlementStatementItem> page) {
        return page.content().stream().map(SettlementStatementItem::operationId).toList();
    }

    @Test
    void listsTheOperationsOfAnAssignorNewestFirstByDefault() {
        PageResponse<SettlementStatementItem> page = statement("");

        assertThat(ids(page)).containsExactly(jan11Settled, jan10MidnightCancelled, jan9LastSecondPending, jan9Settled);
        assertThat(page.page()).isEqualTo(new PageResponse.PageMetadata(0, 20, 4, 1));
        SettlementStatementItem first = page.content().getFirst();
        assertThat(first.assignorName()).isEqualTo("Relatório " + assignorId);
        assertThat(first.assignorDocument()).hasSize(14);
        assertThat(first.totalNetAmount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(first.totalFaceValue()).isEqualTo(new BigDecimal("60.00"));
        assertThat(first.totalDiscount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(first.receivablesCount()).isEqualTo(1);
        assertThat(first.createdAt()).isEqualTo(Instant.parse("2026-01-11T15:00:00Z"));
        assertThat(first.settledAt()).isEqualTo(Instant.parse("2026-01-11T16:00:00Z"));
    }

    @Test
    void periodBoundariesFollowTheBusinessTimeZone() {
        assertThat(ids(statement("&from=2026-01-09&to=2026-01-09")))
                .containsExactly(jan9LastSecondPending, jan9Settled);
        assertThat(ids(statement("&from=2026-01-10&to=2026-01-10"))).containsExactly(jan10MidnightCancelled);
        assertThat(ids(statement("&from=2026-01-10"))).containsExactly(jan11Settled, jan10MidnightCancelled);
        assertThat(ids(statement("&to=2026-01-09"))).containsExactly(jan9LastSecondPending, jan9Settled);
        assertThat(ids(statement("&from=2026-01-12"))).isEmpty();
    }

    @Test
    void filtersByCurrencyAndStatus() {
        assertThat(ids(statement("&currency=USD"))).containsExactly(jan9LastSecondPending);
        assertThat(ids(statement("&status=SETTLED"))).containsExactly(jan11Settled, jan9Settled);
        assertThat(ids(statement("&status=CANCELLED"))).containsExactly(jan10MidnightCancelled);
    }

    @Test
    void combinesFilters() {
        assertThat(ids(statement("&status=SETTLED&currency=BRL&from=2026-01-10&to=2026-01-31")))
                .containsExactly(jan11Settled);
        assertThat(ids(statement("&status=PENDING&currency=BRL"))).isEmpty();
    }

    @Test
    void paginatesOnTheServer() {
        PageResponse<SettlementStatementItem> second = statement("&size=3&page=1");

        assertThat(ids(second)).containsExactly(jan9Settled);
        assertThat(second.page()).isEqualTo(new PageResponse.PageMetadata(1, 3, 4, 2));

        PageResponse<SettlementStatementItem> beyond = statement("&size=3&page=5");
        assertThat(beyond.content()).isEmpty();
        assertThat(beyond.page().totalElements()).isEqualTo(4);
    }

    @Test
    void sortsByWhitelistedFields() {
        assertThat(ids(statement("&sort=totalNetAmount,asc")))
                .containsExactly(jan11Settled, jan9Settled, jan9LastSecondPending, jan10MidnightCancelled);
        assertThat(ids(statement("&sort=createdAt,asc")))
                .containsExactly(jan9Settled, jan9LastSecondPending, jan10MidnightCancelled, jan11Settled);
        // operations that were never settled go last in both directions
        assertThat(ids(statement("&sort=settledAt,desc")).subList(0, 2)).containsExactly(jan11Settled, jan9Settled);
        assertThat(ids(statement("&sort=settledAt,asc")).subList(0, 2)).containsExactly(jan9Settled, jan11Settled);
        assertThat(statement("&sort=assignorName,asc").content()).hasSize(4);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "sort=password,asc",
                "sort=createdAt,sideways",
                "sort=created_at;DROP TABLE credit_assignment",
                "size=101",
                "page=-1",
                "from=2026-02-01&to=2026-01-01",
                "from=01/02/2026",
                "currency=EUR",
                "status=PAID",
                "assignorId=42"
            })
    void rejectsInvalidParameters(String query) {
        client.get()
                .uri("/api/v1/reports/settlement-statement?" + query)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void worksWithoutAnyFilter() {
        PageResponse<SettlementStatementItem> all = client.get()
                .uri("/api/v1/reports/settlement-statement?size=5")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE)
                .returnResult()
                .getResponseBody();

        assertThat(all.page().totalElements()).isGreaterThanOrEqualTo(4);
        assertThat(all.content()).hasSizeLessThanOrEqualTo(5);
    }
}
