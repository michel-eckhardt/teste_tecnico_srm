package com.srm.creditengine.domain.currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Rate published by an external provider, before it is persisted.
 *
 * @param rate units of {@code quote} per unit of {@code base}
 * @param referenceDate date the provider assigns to the rate (e.g. the ECB publication date)
 */
public record ProvidedRate(CurrencyCode base, CurrencyCode quote, BigDecimal rate, LocalDate referenceDate) {

    public ProvidedRate {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(quote, "quote");
        Objects.requireNonNull(rate, "rate");
        Objects.requireNonNull(referenceDate, "referenceDate");
    }
}
