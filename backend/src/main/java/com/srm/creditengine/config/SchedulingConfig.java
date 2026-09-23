package com.srm.creditengine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables {@code @Scheduled} jobs (e.g. the exchange rate synchronization). */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchedulingConfig {}
