package com.srm.creditengine.domain.currency;

import static com.srm.creditengine.domain.currency.CurrencyCode.BRL;
import static com.srm.creditengine.domain.currency.CurrencyCode.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ExchangeRateTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 23);
    private static final Instant NOW = Instant.parse("2026-09-23T14:05:00Z");

    @Test
    void normalizesTheRateToEightDecimalPlaces() {
        ExchangeRate rate = ExchangeRate.of(USD, BRL, new BigDecimal("5.1322"), ExchangeRateSource.MANUAL, DATE, NOW);

        assertThat(rate.getRate()).isEqualTo(new BigDecimal("5.13220000"));
        assertThat(rate.links(BRL, USD)).isTrue();
        assertThat(rate.links(USD, BRL)).isTrue();
    }

    @Test
    void rejectsNonPositiveRates() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ExchangeRate.of(USD, BRL, BigDecimal.ZERO, ExchangeRateSource.MANUAL, DATE, NOW));
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> ExchangeRate.of(USD, BRL, new BigDecimal("-5"), ExchangeRateSource.MANUAL, DATE, NOW));
    }

    @Test
    void rejectsARateBetweenTheSameCurrency() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ExchangeRate.of(USD, USD, BigDecimal.ONE, ExchangeRateSource.MANUAL, DATE, NOW));
    }

    @Test
    void rejectsMorePrecisionThanTheColumnCanStoreInsteadOfSilentlyRounding() {
        assertThatIllegalArgumentException()
                .isThrownBy(() ->
                        ExchangeRate.of(USD, BRL, new BigDecimal("5.123456789"), ExchangeRateSource.MANUAL, DATE, NOW));
    }
}
