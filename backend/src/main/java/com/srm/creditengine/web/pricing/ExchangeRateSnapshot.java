package com.srm.creditengine.web.pricing;

import com.srm.creditengine.domain.currency.CurrencyCode;
import com.srm.creditengine.domain.currency.CurrencyConversion;
import com.srm.creditengine.domain.currency.ExchangeRate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Persisted rate used to convert a receivable, exactly as stored ({@code rate} = quote per base).
 * A BRL receivable paid in USD therefore shows the USD/BRL row, applied as a division.
 */
public record ExchangeRateSnapshot(
        UUID id, CurrencyCode base, CurrencyCode quote, BigDecimal rate, LocalDate referenceDate) {

    public static @Nullable ExchangeRateSnapshot from(@Nullable CurrencyConversion conversion) {
        if (conversion == null) {
            return null;
        }
        ExchangeRate rate = conversion.rate();
        return new ExchangeRateSnapshot(
                rate.getId(), rate.getBaseCurrency(), rate.getQuoteCurrency(), rate.getRate(), rate.getReferenceDate());
    }
}
