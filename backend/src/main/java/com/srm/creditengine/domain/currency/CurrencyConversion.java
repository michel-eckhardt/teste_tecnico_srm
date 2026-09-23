package com.srm.creditengine.domain.currency;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Conversion from one currency to another backed by a persisted {@link ExchangeRate}, used either
 * in the direction it was published (multiply) or as its derived inverse (divide).
 *
 * <p>The inverse is never materialized as a rounded rate for calculations: dividing by the stored
 * rate with {@link MathContext#DECIMAL128} keeps the conversion exact to 34 significant digits, and
 * callers round only once, at the end, to the minor units of the target currency.
 *
 * @param rate the persisted observation
 * @param from currency of the amounts being converted
 * @param to target currency
 */
public record CurrencyConversion(ExchangeRate rate, CurrencyCode from, CurrencyCode to) {

    public CurrencyConversion {
        Objects.requireNonNull(rate, "rate");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from == to || !rate.links(from, to)) {
            throw new IllegalArgumentException("rate %s/%s cannot convert %s to %s"
                    .formatted(rate.getBaseCurrency(), rate.getQuoteCurrency(), from, to));
        }
    }

    /** {@code true} when converting against the published direction (quote to base). */
    public boolean inverse() {
        return from == rate.getQuoteCurrency();
    }

    /** Converts an amount of {@code from} into {@code to}, without rounding. */
    public BigDecimal convert(BigDecimal amount) {
        return inverse()
                ? amount.divide(rate.getRate(), MathContext.DECIMAL128)
                : amount.multiply(rate.getRate(), MathContext.DECIMAL128);
    }

    /** Units of {@code to} per unit of {@code from}, rounded to the rate scale (display only). */
    public BigDecimal effectiveRate() {
        return inverse()
                ? BigDecimal.ONE.divide(rate.getRate(), ExchangeRate.SCALE, RoundingMode.HALF_EVEN)
                : rate.getRate();
    }
}
