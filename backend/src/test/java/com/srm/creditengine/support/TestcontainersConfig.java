package com.srm.creditengine.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL 17 in a container, wired into the datasource through {@link ServiceConnection}. Shared by
 * every integration test (Spring caches the context) and by {@code TestSrmCreditEngineApplication}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    public static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:17-alpine");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
