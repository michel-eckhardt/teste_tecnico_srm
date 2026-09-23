package com.srm.creditengine.domain.common;

import jakarta.validation.constraints.NotNull;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Business calendar settings.
 *
 * @param zoneId time zone that defines the business date (operation date, report periods)
 */
@Validated
@ConfigurationProperties("srm.business")
public record BusinessProperties(
        @NotNull @DefaultValue("America/Sao_Paulo") ZoneId zoneId) {}
