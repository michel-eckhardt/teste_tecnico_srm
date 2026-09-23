package com.srm.creditengine.web.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.srm.creditengine.domain.currency.CurrencyCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/** {@code base}/{@code quote} query parameters identifying a currency pair. */
public record CurrencyPairQuery(
        @NotNull @Schema(example = "USD") CurrencyCode base,
        @NotNull @Schema(example = "BRL") CurrencyCode quote) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "base e quote devem ser moedas diferentes")
    public boolean isDistinctCurrencies() {
        return base == null || base != quote;
    }
}
