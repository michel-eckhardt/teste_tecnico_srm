package com.srm.creditengine.integration.fx.frankfurter;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Frankfurter API client settings (public API, no key; ECB reference rates).
 *
 * @param baseUrl API root, e.g. {@code https://api.frankfurter.dev/v1}
 * @param connectTimeout maximum time to establish the TCP/TLS connection
 * @param readTimeout maximum time to wait for the response
 */
@Validated
@ConfigurationProperties("srm.fx.frankfurter")
public record FrankfurterProperties(
        @NotNull @DefaultValue("https://api.frankfurter.dev/v1")
        URI baseUrl,

        @NotNull @DefaultValue("2s") Duration connectTimeout,
        @NotNull @DefaultValue("3s") Duration readTimeout) {}
