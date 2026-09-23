package com.srm.creditengine.web.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Browser origins allowed to call the API (the operator SPA).
 *
 * @param allowedOrigins exact origins, e.g. {@code http://localhost:5173}
 */
@ConfigurationProperties("srm.web.cors")
public record CorsProperties(
        @DefaultValue({"http://localhost:5173", "http://localhost:3000"})
        List<String> allowedOrigins) {}
