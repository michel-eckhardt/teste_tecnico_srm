package com.srm.creditengine.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single source of time for the application. Every component receives this {@link Clock} instead
 * of calling {@code now()} directly, so business dates are deterministic in tests.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
