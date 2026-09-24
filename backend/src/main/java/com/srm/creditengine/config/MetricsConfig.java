package com.srm.creditengine.config;

import com.srm.creditengine.domain.common.BusinessMetrics;
import com.srm.creditengine.domain.currency.CurrencyCode;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class MetricsConfig {

    /** Business meters with one series per supported payment currency registered upfront. */
    @Bean
    BusinessMetrics businessMetrics(MeterRegistry registry) {
        return new BusinessMetrics(
                registry, Arrays.stream(CurrencyCode.values()).map(Enum::name).toList());
    }
}
