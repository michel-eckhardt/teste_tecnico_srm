package com.srm.creditengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SrmCreditEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(SrmCreditEngineApplication.class, args);
    }
}
