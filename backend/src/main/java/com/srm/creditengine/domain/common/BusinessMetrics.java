package com.srm.creditengine.domain.common;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Business meters of the credit engine (exported to Prometheus with the common tag
 * {@code application}). Names follow Micrometer conventions; Prometheus renders them with
 * underscores and a {@code _total} suffix for counters. Names never end in {@code .created}: the
 * Prometheus client reserves the {@code _created} suffix and would silently strip it.
 */
@Component
public class BusinessMetrics {

    public static final String ASSIGNMENTS_CREATED = "srm.credit_assignments.opened";
    public static final String ASSIGNMENTS_SETTLED = "srm.credit_assignments.settled";
    public static final String SETTLEMENT_CONFLICTS = "srm.credit_assignments.settlement.conflicts";
    public static final String PRICING_DURATION = "srm.pricing.duration";
    public static final String FX_PROVIDER_CALLS = "srm.fx.provider.calls";

    private final MeterRegistry registry;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void creditAssignmentCreated(String paymentCurrency) {
        Counter.builder(ASSIGNMENTS_CREATED)
                .description("Credit assignments created (idempotent replays excluded)")
                .tag("payment_currency", paymentCurrency)
                .register(registry)
                .increment();
    }

    public void creditAssignmentSettled(String paymentCurrency) {
        Counter.builder(ASSIGNMENTS_SETTLED)
                .description("Credit assignments settled (fund debited)")
                .tag("payment_currency", paymentCurrency)
                .register(registry)
                .increment();
    }

    /**
     * @param reason {@code stale_version}, {@code already_settled}, {@code invalid_state} or
     *     {@code optimistic_lock} (one per collision on a row version, retried transparently)
     */
    public void settlementConflict(String reason) {
        Counter.builder(SETTLEMENT_CONFLICTS)
                .description("Settlement/cancellation commands rejected or retried because of a conflict")
                .tag("reason", reason)
                .register(registry)
                .increment();
    }

    /** Times a pricing batch. */
    public <T> T timePricing(String paymentCurrency, Supplier<T> pricing) {
        return Timer.builder(PRICING_DURATION)
                .description("Time to price a batch of receivables (simulation or credit assignment)")
                .tag("payment_currency", paymentCurrency)
                .publishPercentileHistogram()
                .register(registry)
                .record(pricing);
    }

    /**
     * @param outcome {@code success}, {@code failure}, {@code rejected} (open circuit) or
     *     {@code invalid_payload}
     */
    public void fxProviderCall(String provider, String outcome, Duration duration) {
        Timer.builder(FX_PROVIDER_CALLS)
                .description("Calls to the external exchange rate provider, including retries")
                .tag("provider", provider)
                .tag("outcome", outcome)
                .register(registry)
                .record(duration);
    }
}
