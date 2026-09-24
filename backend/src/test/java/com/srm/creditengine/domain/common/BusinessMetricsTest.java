package com.srm.creditengine.domain.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.domain.common.BusinessMetrics.ConflictReason;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class BusinessMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final BusinessMetrics metrics = new BusinessMetrics(registry, List.of("BRL", "USD"));

    private double count(String name, String tag, String value) {
        return registry.get(name).tag(tag, value).counter().count();
    }

    @Test
    void registersEveryCounterSeriesAtZeroSoPrometheusSeesTheFirstIncrement() {
        for (String currency : List.of("BRL", "USD")) {
            assertThat(count(BusinessMetrics.ASSIGNMENTS_CREATED, "payment_currency", currency))
                    .isZero();
            assertThat(count(BusinessMetrics.ASSIGNMENTS_SETTLED, "payment_currency", currency))
                    .isZero();
        }
        assertThat(registry.get(BusinessMetrics.SETTLEMENT_CONFLICTS).counters())
                .hasSize(ConflictReason.values().length)
                .allSatisfy(counter -> assertThat(counter.count()).isZero());
    }

    @Test
    void countsByPaymentCurrencyAndLowerCaseConflictReason() {
        metrics.creditAssignmentCreated("USD");
        metrics.creditAssignmentSettled("USD");
        metrics.settlementConflict(ConflictReason.OPTIMISTIC_LOCK);

        assertThat(count(BusinessMetrics.ASSIGNMENTS_CREATED, "payment_currency", "USD"))
                .isEqualTo(1);
        assertThat(count(BusinessMetrics.ASSIGNMENTS_SETTLED, "payment_currency", "USD"))
                .isEqualTo(1);
        assertThat(count(BusinessMetrics.SETTLEMENT_CONFLICTS, "reason", "optimistic_lock"))
                .isEqualTo(1);
    }
}
