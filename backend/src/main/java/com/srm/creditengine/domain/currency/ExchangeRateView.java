package com.srm.creditengine.domain.currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Exchange rate as seen by a client asking for a {@code base/quote} pair.
 *
 * @param id identifier of the persisted observation backing this rate
 * @param rate units of {@code quote} per unit of {@code base} (scale 8)
 * @param derived {@code true} when the stored observation is the inverse pair
 * @param stale {@code true} when the rate is older than the configured maximum age
 */
public record ExchangeRateView(
        UUID id,
        CurrencyCode base,
        CurrencyCode quote,
        BigDecimal rate,
        ExchangeRateSource source,
        LocalDate referenceDate,
        Instant createdAt,
        boolean derived,
        boolean stale) {

    static ExchangeRateView of(CurrencyConversion conversion, boolean stale) {
        ExchangeRate rate = conversion.rate();
        return new ExchangeRateView(
                rate.getId(),
                conversion.from(),
                conversion.to(),
                conversion.effectiveRate(),
                rate.getSource(),
                rate.getReferenceDate(),
                rate.getCreatedAt(),
                conversion.inverse(),
                stale);
    }

    static ExchangeRateView of(ExchangeRate rate, boolean stale) {
        return of(new CurrencyConversion(rate, rate.getBaseCurrency(), rate.getQuoteCurrency()), stale);
    }
}
