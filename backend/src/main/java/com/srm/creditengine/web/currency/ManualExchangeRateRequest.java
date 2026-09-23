package com.srm.creditengine.web.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.srm.creditengine.domain.currency.CurrencyCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Manual exchange rate update.
 *
 * @param rate units of {@code quote} per unit of {@code base}, up to 8 decimal places
 * @param referenceDate business date of the rate (defaults to today)
 */
public record ManualExchangeRateRequest(
        @NotNull @Schema(example = "USD") CurrencyCode base,
        @NotNull @Schema(example = "BRL") CurrencyCode quote,

        @NotNull @Positive @Digits(integer = 11, fraction = 8) @Schema(example = "5.2000")
        BigDecimal rate,

        @PastOrPresent @Schema(example = "2026-09-23") LocalDate referenceDate) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "base e quote devem ser moedas diferentes")
    public boolean isDistinctCurrencies() {
        return base == null || base != quote;
    }
}
