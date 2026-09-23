package com.srm.creditengine.web.currency;

import com.srm.creditengine.domain.currency.CurrencyCode;
import com.srm.creditengine.domain.currency.ExchangeRateSource;
import com.srm.creditengine.domain.currency.ExchangeRateView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExchangeRateResponse(
        UUID id,
        CurrencyCode base,
        CurrencyCode quote,

        @Schema(description = "Unidades de quote por 1 unidade de base", example = "5.13220000")
        BigDecimal rate,

        ExchangeRateSource source,
        LocalDate referenceDate,
        Instant createdAt,

        @Schema(description = "Taxa mais antiga que srm.fx.max-rate-age; não pode precificar operações")
        boolean stale,

        @Schema(description = "Taxa derivada do par inverso (ex.: BRL/USD calculada a partir de USD/BRL)")
        boolean derived) {

    public static ExchangeRateResponse from(ExchangeRateView view) {
        return new ExchangeRateResponse(
                view.id(),
                view.base(),
                view.quote(),
                view.rate(),
                view.source(),
                view.referenceDate(),
                view.createdAt(),
                view.stale(),
                view.derived());
    }
}
