package com.srm.creditengine.web.config;

import com.srm.creditengine.web.support.ApiPaths;
import com.srm.creditengine.web.support.CorrelationIdFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS for the operator SPA. Besides the simple headers, the browser must be allowed to send the
 * concurrency/idempotency headers and to read the ones the API returns (ETag, Location, correlation).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CorsProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    public WebConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(ApiPaths.V1 + "/**")
                .allowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders(
                        HttpHeaders.CONTENT_TYPE,
                        HttpHeaders.ACCEPT,
                        HttpHeaders.IF_MATCH,
                        "Idempotency-Key",
                        CorrelationIdFilter.HEADER)
                .exposedHeaders(HttpHeaders.ETAG, HttpHeaders.LOCATION, CorrelationIdFilter.HEADER)
                .maxAge(3600);
    }
}
