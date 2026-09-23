package com.srm.creditengine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/**
 * Enables Spring Framework 7 resilience annotations ({@code @Retryable}, {@code @ConcurrencyLimit})
 * used for the optimistic locking retries of settlements. Calls to external systems use
 * Resilience4j instead (circuit breaker and metrics).
 */
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
public class ResilienceConfig {}
