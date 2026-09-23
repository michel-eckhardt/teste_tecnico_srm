package com.srm.creditengine.domain.currency;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Currency engine settings.
 *
 * @param maxRateAge how old (from the start of its reference date, in the business zone) a rate may
 *     be and still price a cross-currency operation. ECB rates are published on business days only,
 *     so the default of 5 days covers weekends and long holidays.
 */
@Validated
@ConfigurationProperties("srm.fx")
public record FxProperties(@NotNull @DefaultValue("P5D") Duration maxRateAge) {}
