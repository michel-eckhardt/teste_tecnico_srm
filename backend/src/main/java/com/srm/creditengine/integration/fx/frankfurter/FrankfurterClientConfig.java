package com.srm.creditengine.integration.fx.frankfurter;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Dedicated {@link RestClient} for Frankfurter. Built from the Boot-managed builder (JSON mapper
 * and observability instrumentation) with explicit timeouts: an external call must never hang a
 * thread for the default (infinite) read timeout.
 */
@Configuration(proxyBeanMethods = false)
class FrankfurterClientConfig {

    @Bean
    RestClient frankfurterRestClient(RestClient.Builder builder, FrankfurterProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return builder.baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
