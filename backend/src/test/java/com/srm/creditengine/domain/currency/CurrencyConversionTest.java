package com.srm.creditengine.domain.currency;

import static com.srm.creditengine.domain.currency.CurrencyCode.BRL;
import static com.srm.creditengine.domain.currency.CurrencyCode.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CurrencyConversionTest {

    private static final ExchangeRate USD_BRL = ExchangeRate.of(
            USD,
            BRL,
            new BigDecimal("5.1322"),
            ExchangeRateSource.FRANKFURTER,
            LocalDate.of(2026, 9, 23),
            Instant.parse("2026-09-23T14:05:00Z"));

    @Test
    void multipliesInThePublishedDirection() {
        CurrencyConversion usdToBrl = new CurrencyConversion(USD_BRL, USD, BRL);

        assertThat(usdToBrl.inverse()).isFalse();
        assertThat(usdToBrl.convert(new BigDecimal("2500.00"))).isEqualByComparingTo("12830.50");
        assertThat(usdToBrl.effectiveRate()).isEqualTo(new BigDecimal("5.13220000"));
    }

    @Test
    void dividesByTheStoredRateForTheDerivedInverse() {
        CurrencyConversion brlToUsd = new CurrencyConversion(USD_BRL, BRL, USD);

        // 10000 / 1.025^3 = 9285.99410... BRL (unrounded present value of the contract example)
        BigDecimal presentValue =
                new BigDecimal("10000").divide(new BigDecimal("1.025").pow(3), MathContext.DECIMAL128);

        assertThat(brlToUsd.inverse()).isTrue();
        assertThat(brlToUsd.convert(presentValue).setScale(2, RoundingMode.HALF_EVEN))
                .isEqualTo(new BigDecimal("1809.36"));
    }

    @Test
    void inverseRateIsOnlyRoundedForDisplay() {
        CurrencyConversion brlToUsd = new CurrencyConversion(USD_BRL, BRL, USD);

        assertThat(brlToUsd.effectiveRate()).isEqualTo(new BigDecimal("0.19484821"));
        // the calculation itself keeps 34 significant digits instead of using the rounded rate
        assertThat(brlToUsd.convert(new BigDecimal("100000000.00")).setScale(2, RoundingMode.HALF_EVEN))
                .isEqualTo(new BigDecimal("19484821.32"));
    }

    @Test
    void roundTripIsLosslessAtDecimal128Precision() {
        BigDecimal amount = new BigDecimal("123456789.12");
        BigDecimal inBrl = new CurrencyConversion(USD_BRL, USD, BRL).convert(amount);
        BigDecimal backInUsd = new CurrencyConversion(USD_BRL, BRL, USD).convert(inBrl);

        assertThat(backInUsd.round(new MathContext(30))).isEqualByComparingTo(amount);
    }

    @Test
    void rejectsConversionsTheRateCannotPerform() {
        assertThatIllegalArgumentException().isThrownBy(() -> new CurrencyConversion(USD_BRL, BRL, BRL));
    }
}
