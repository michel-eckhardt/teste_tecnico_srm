package com.srm.creditengine;

import com.srm.creditengine.support.TestcontainersConfig;
import org.springframework.boot.SpringApplication;

/**
 * Runs the application locally against a throw-away PostgreSQL container:
 * {@code ./mvnw spring-boot:test-run}. No local database installation is required.
 */
public class TestSrmCreditEngineApplication {

    public static void main(String[] args) {
        SpringApplication.from(SrmCreditEngineApplication::main)
                .with(TestcontainersConfig.class)
                .run(args);
    }
}
