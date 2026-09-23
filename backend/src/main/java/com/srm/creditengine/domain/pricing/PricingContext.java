package com.srm.creditengine.domain.pricing;

import com.srm.creditengine.domain.currency.CurrencyCode;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Everything a {@link PricingStrategy} may use to decide the risk spread of a receivable. Current
 * strategies use a fixed spread, but the context lets a new rule depend on term, amount or currency
 * without changing the strategy contract.
 *
 * @param termDays calendar days between the operation date and the due date
 * @param termMonths {@code termDays / 30}, unrounded
 */
public record PricingContext(
        ReceivableType type,
        BigDecimal faceValue,
        CurrencyCode faceCurrency,
        LocalDate operationDate,
        LocalDate dueDate,
        long termDays,
        BigDecimal termMonths) {}
