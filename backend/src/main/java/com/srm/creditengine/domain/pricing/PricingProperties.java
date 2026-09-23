package com.srm.creditengine.domain.pricing;

import com.srm.creditengine.domain.currency.CurrencyCode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Pricing parameters (the per-type spreads belong to each {@link PricingStrategy}).
 *
 * @param maxTermDays longest accepted term between the operation date and the due date
 * @param baseRates monthly base rate (funding cost) per face currency, e.g. BRL 0.01 = 1% a.m.
 */
@Validated
@ConfigurationProperties("srm.pricing")
public record PricingProperties(
        @Positive @DefaultValue("1825") int maxTermDays,
        @NotEmpty Map<CurrencyCode, @NotNull @PositiveOrZero BigDecimal> baseRates) {}
