package com.srm.creditengine.domain.assignment;

import static com.srm.creditengine.domain.currency.CurrencyCode.BRL;
import static com.srm.creditengine.domain.currency.CurrencyCode.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.srm.creditengine.domain.assignor.Assignor;
import com.srm.creditengine.domain.common.ErrorCode;
import com.srm.creditengine.domain.common.StaleVersionException;
import com.srm.creditengine.domain.currency.CurrencyCode;
import com.srm.creditengine.domain.currency.CurrencyConversion;
import com.srm.creditengine.domain.currency.ExchangeRate;
import com.srm.creditengine.domain.currency.ExchangeRateSource;
import com.srm.creditengine.domain.pricing.PricedReceivable;
import com.srm.creditengine.domain.pricing.ReceivableType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CreditAssignmentTest {

    private static final Instant NOW = Instant.parse("2026-09-23T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 23);
    private static final Assignor ACME = Assignor.register("ACME Indústria Ltda", "11222333000181", NOW);
    private static final ExchangeRate USD_BRL =
            ExchangeRate.of(USD, BRL, new BigDecimal("5.1322"), ExchangeRateSource.FRANKFURTER, TODAY, NOW);

    /** Contract example: 10000.00 BRL duplicata (90 days) + 2500.00 USD cheque (48 days) paid in BRL. */
    static List<PricedReceivable> contractExample() {
        return List.of(
                priced(
                        ReceivableType.DUPLICATA_MERCANTIL,
                        "10000.00",
                        BRL,
                        90,
                        "9285.99",
                        "714.01",
                        null,
                        "9285.99",
                        "10000.00"),
                priced(
                        ReceivableType.CHEQUE_PRE_DATADO,
                        "2500.00",
                        USD,
                        48,
                        "2384.52",
                        "115.48",
                        new CurrencyConversion(USD_BRL, USD, BRL),
                        "12237.82",
                        "12830.50"));
    }

    static PricedReceivable priced(
            ReceivableType type,
            String face,
            CurrencyCode faceCurrency,
            int days,
            String presentValue,
            String discount,
            CurrencyConversion conversion,
            String net,
            String faceInBrl) {
        return new PricedReceivable(
                type,
                new BigDecimal(face),
                faceCurrency,
                BRL,
                TODAY,
                TODAY.plusDays(days),
                days,
                BigDecimal.valueOf(days).divide(BigDecimal.valueOf(30), 8, java.math.RoundingMode.HALF_EVEN),
                new BigDecimal(faceCurrency == BRL ? "0.01000000" : "0.00500000"),
                new BigDecimal(type == ReceivableType.DUPLICATA_MERCANTIL ? "0.01500000" : "0.02500000"),
                new BigDecimal("0.03"),
                new BigDecimal(presentValue),
                new BigDecimal(discount),
                conversion,
                new BigDecimal(net),
                new BigDecimal(faceInBrl));
    }

    private static CreditAssignment pending() {
        return CreditAssignment.open(ACME, BRL, contractExample(), null, NOW);
    }

    @Test
    void opensAPendingOperationWithTotalsInThePaymentCurrency() {
        CreditAssignment operation =
                CreditAssignment.open(ACME, BRL, contractExample(), new IdempotencyKey("key-1", "a".repeat(64)), NOW);

        assertThat(operation.getStatus()).isEqualTo(CreditAssignmentStatus.PENDING);
        assertThat(operation.getReceivablesCount()).isEqualTo(2);
        assertThat(operation.getTotalFaceValue()).isEqualTo(new BigDecimal("22830.50"));
        assertThat(operation.getTotalNetAmount()).isEqualTo(new BigDecimal("21523.81"));
        assertThat(operation.getTotalDiscount()).isEqualTo(new BigDecimal("1306.69"));
        assertThat(operation.getTotalFaceValue())
                .isEqualTo(operation.getTotalDiscount().add(operation.getTotalNetAmount()));
        assertThat(operation.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(operation.getCreatedAt()).isEqualTo(NOW);
        assertThat(operation.getSettledAt()).isNull();
    }

    @Test
    void keepsAnAuditSnapshotOfTheRatesUsedByEachReceivable() {
        List<Receivable> receivables = pending().getReceivables();

        assertThat(receivables.getFirst().getExchangeRate()).isNull();
        assertThat(receivables.getFirst().getExchangeRateApplied()).isNull();
        Receivable crossCurrency = receivables.get(1);
        assertThat(crossCurrency.getExchangeRate()).isSameAs(USD_BRL);
        assertThat(crossCurrency.getExchangeRateApplied()).isEqualTo(new BigDecimal("5.13220000"));
        assertThat(crossCurrency.getSpread()).isEqualTo(new BigDecimal("0.02500000"));
        assertThat(crossCurrency.getBaseRate()).isEqualTo(new BigDecimal("0.00500000"));
        assertThat(crossCurrency.getTermDays()).isEqualTo(48);
        assertThat(crossCurrency.getNetAmount()).isEqualTo(new BigDecimal("12237.82"));
    }

    @Test
    void rejectsEmptyOrOversizedBatches() {
        assertThatIllegalArgumentException().isThrownBy(() -> CreditAssignment.open(ACME, BRL, List.of(), null, NOW));
        List<PricedReceivable> tooMany =
                Collections.nCopies(501, contractExample().getFirst());
        assertThatIllegalArgumentException().isThrownBy(() -> CreditAssignment.open(ACME, BRL, tooMany, null, NOW));
    }

    @Test
    void rejectsReceivablesPricedForAnotherPaymentCurrency() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CreditAssignment.open(ACME, USD, contractExample(), null, NOW));
    }

    @Nested
    class Lifecycle {

        @Test
        void settlesAPendingOperation() {
            CreditAssignment operation = pending();

            operation.settle(NOW.plusSeconds(60));

            assertThat(operation.getStatus()).isEqualTo(CreditAssignmentStatus.SETTLED);
            assertThat(operation.getSettledAt()).isEqualTo(NOW.plusSeconds(60));
        }

        @Test
        void refusesToSettleTwice() {
            CreditAssignment operation = pending();
            operation.settle(NOW);

            assertThatExceptionOfType(OperationAlreadySettledException.class)
                    .isThrownBy(() -> operation.settle(NOW))
                    .satisfies(ex -> assertThat(ex.code()).isEqualTo(ErrorCode.OPERATION_ALREADY_SETTLED));
        }

        @Test
        void refusesToSettleACancelledOperation() {
            CreditAssignment operation = pending();
            operation.cancel(NOW);

            assertThatExceptionOfType(InvalidStateTransitionException.class)
                    .isThrownBy(() -> operation.settle(NOW))
                    .withMessageContaining("CANCELLED")
                    .satisfies(ex -> assertThat(ex.code()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
            assertThat(operation.getSettledAt()).isNull();
        }

        @Test
        void cancelsAPendingOperation() {
            CreditAssignment operation = pending();

            operation.cancel(NOW);

            assertThat(operation.getStatus()).isEqualTo(CreditAssignmentStatus.CANCELLED);
            assertThat(operation.getCancelledAt()).isEqualTo(NOW);
        }

        @Test
        void refusesToCancelASettledOrCancelledOperation() {
            CreditAssignment settled = pending();
            settled.settle(NOW);
            CreditAssignment cancelled = pending();
            cancelled.cancel(NOW);

            assertThatExceptionOfType(InvalidStateTransitionException.class).isThrownBy(() -> settled.cancel(NOW));
            assertThatExceptionOfType(InvalidStateTransitionException.class).isThrownBy(() -> cancelled.cancel(NOW));
        }

        @Test
        void rejectsCommandsBasedOnAnotherVersion() {
            CreditAssignment operation = pending();

            operation.requireVersion(0);
            assertThatExceptionOfType(StaleVersionException.class)
                    .isThrownBy(() -> operation.requireVersion(3))
                    .satisfies(ex -> assertThat(ex.code()).isEqualTo(ErrorCode.PRECONDITION_FAILED));
        }
    }

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
        "PENDING,   SETTLED,   true",
        "PENDING,   CANCELLED, true",
        "PENDING,   PENDING,   false",
        "SETTLED,   CANCELLED, false",
        "SETTLED,   PENDING,   false",
        "SETTLED,   SETTLED,   false",
        "CANCELLED, SETTLED,   false",
        "CANCELLED, PENDING,   false",
        "CANCELLED, CANCELLED, false"
    })
    void statusMachineOnlyAllowsLeavingPending(
            CreditAssignmentStatus from, CreditAssignmentStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }
}
