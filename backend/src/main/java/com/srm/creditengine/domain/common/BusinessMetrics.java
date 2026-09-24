package com.srm.creditengine.domain.common;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Business meters of the credit engine (exported to Prometheus with the common tag
 * {@code application}). Names follow Micrometer conventions; Prometheus renders them with
 * underscores and a {@code _total} suffix for counters. Names never end in {@code .created}: the
 * Prometheus client reserves the {@code _created} suffix and would silently strip it.
 */
public class BusinessMetrics {

    public static final String ASSIGNMENTS_CREATED = "srm.credit_assignments.opened";
    public static final String ASSIGNMENTS_SETTLED = "srm.credit_assignments.settled";
    public static final String SETTLEMENT_CONFLICTS = "srm.credit_assignments.settlement.conflicts";
    public static final String PRICING_DURATION = "srm.pricing.duration";
    public static final String FX_PROVIDER_CALLS = "srm.fx.provider.calls";

    /** Why a settlement or cancellation command collided with another change. */
    public enum ConflictReason {
        /** The client's If-Match is not the current version. */
        STALE_VERSION,
        /** The operation is already settled. */
        ALREADY_SETTLED,
        /** The operation is in a state that does not allow the transition (e.g. cancelled). */
        INVALID_STATE,
        /** Row version collision detected on write (retried transparently). */
        OPTIMISTIC_LOCK;

        String tag() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final MeterRegistry registry;

    /**
     * Registers every counter series at zero upfront: Prometheus cannot see the jump of a series
     * that appears with its first increment already counted, so {@code rate()}/{@code increase()}
     * would miss the first events of each currency or conflict reason.
     *
     * @param paymentCurrencies ISO codes of the supported payment currencies (wired in {@code
     *     config}, so this shared module does not depend on the currency module)
     */
    public BusinessMetrics(MeterRegistry registry, Collection<String> paymentCurrencies) {
        this.registry = registry;
        for (String currency : paymentCurrencies) {
            createdCounter(currency);
            settledCounter(currency);
        }
        for (ConflictReason reason : ConflictReason.values()) {
            conflictCounter(reason);
        }
    }

    public void creditAssignmentCreated(String paymentCurrency) {
        createdCounter(paymentCurrency).increment();
    }

    public void creditAssignmentSettled(String paymentCurrency) {
        settledCounter(paymentCurrency).increment();
    }

    /** One per collision on a row version, including the ones retried transparently. */
    public void settlementConflict(ConflictReason reason) {
        conflictCounter(reason).increment();
    }

    private Counter createdCounter(String paymentCurrency) {
        return Counter.builder(ASSIGNMENTS_CREATED)
                .description("Credit assignments created (idempotent replays excluded)")
                .tag("payment_currency", paymentCurrency)
                .register(registry);
    }

    private Counter settledCounter(String paymentCurrency) {
        return Counter.builder(ASSIGNMENTS_SETTLED)
                .description("Credit assignments settled (fund debited)")
                .tag("payment_currency", paymentCurrency)
                .register(registry);
    }

    private Counter conflictCounter(ConflictReason reason) {
        return Counter.builder(SETTLEMENT_CONFLICTS)
                .description("Settlement/cancellation commands rejected or retried because of a conflict")
                .tag("reason", reason.tag())
                .register(registry);
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
