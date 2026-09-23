package com.srm.creditengine.support;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

/**
 * Stand-in for the Frankfurter API: every integration test context points
 * {@code srm.fx.frankfurter.base-url} to this local server, so no test ever reaches the network.
 */
@TestConfiguration(proxyBeanMethods = false)
public class WireMockConfig {

    @Bean(destroyMethod = "stop")
    WireMockServer frankfurterMock() {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        return server;
    }

    @Bean
    DynamicPropertyRegistrar frankfurterBaseUrl(WireMockServer frankfurterMock) {
        return registry -> registry.add("srm.fx.frankfurter.base-url", () -> frankfurterMock.baseUrl() + "/v1");
    }
}
