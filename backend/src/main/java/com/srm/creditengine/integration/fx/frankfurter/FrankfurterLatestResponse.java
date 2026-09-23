package com.srm.creditengine.integration.fx.frankfurter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Payload of {@code GET /latest?base=USD&symbols=BRL}:
 * {@code {"amount":1.0,"base":"USD","date":"2026-09-23","rates":{"BRL":5.1322}}}. Rates are bound
 * straight to {@link BigDecimal} (never through {@code double}).
 */
record FrankfurterLatestResponse(BigDecimal amount, String base, LocalDate date, Map<String, BigDecimal> rates) {}
