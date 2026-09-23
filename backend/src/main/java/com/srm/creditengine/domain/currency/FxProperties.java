package com.srm.creditengine.domain.currency;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Currency engine settings.
 *
 * @param maxRateAge how old (from the start of its reference date, in the business zone) a rate may
 *     be and still price a cross-currency operation. ECB rates are published on business days only,
 *     so the default of 5 days covers weekends and long holidays.
 * @param sync synchronization with the external rate provider
 */
@Validated
@ConfigurationProperties("srm.fx")
public record FxProperties(
        @NotNull @DefaultValue("P5D") Duration maxRateAge,
        @Valid @NotNull @DefaultValue Sync sync) {

    /**
     * @param enabled whether rates are pulled automatically (on startup and on {@code cron})
     * @param cron schedule of the automatic synchronization (evaluated in the business zone)
     * @param base currency whose rates are requested
     * @param quotes currencies quoted against {@code base}
     */
    public record Sync(
            @DefaultValue("false") boolean enabled,
            @NotBlank @DefaultValue("0 5 * * * *") String cron,
            @NotNull @DefaultValue("USD") CurrencyCode base,
            @NotEmpty @DefaultValue("BRL") Set<CurrencyCode> quotes) {}
}
