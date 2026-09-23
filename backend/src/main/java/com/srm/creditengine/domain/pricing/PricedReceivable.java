package com.srm.creditengine.domain.pricing;

import com.srm.creditengine.domain.currency.CurrencyCode;
import com.srm.creditengine.domain.currency.CurrencyConversion;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/**
 * Result of pricing one receivable, with every input of the formula kept for audit.
 *
 * @param termMonths {@code termDays / 30}, shown with 8 decimal places
 * @param discountRate {@code baseRate + spread} (monthly)
 * @param presentValue {@code faceValue / (1 + discountRate) ^ termMonths}, in the face currency
 * @param discount {@code faceValue - presentValue}, in the face currency
 * @param conversion FX used to pay in another currency, {@code null} for same-currency receivables
 * @param netAmount present value converted to the payment currency
 * @param faceValueInPaymentCurrency face value converted with the same rate (for operation totals)
 */
public record PricedReceivable(
        ReceivableType type,
        BigDecimal faceValue,
        CurrencyCode faceCurrency,
        CurrencyCode paymentCurrency,
        LocalDate operationDate,
        LocalDate dueDate,
        int termDays,
        BigDecimal termMonths,
        BigDecimal baseRate,
        BigDecimal spread,
        BigDecimal discountRate,
        BigDecimal presentValue,
        BigDecimal discount,
        @Nullable CurrencyConversion conversion,
        BigDecimal netAmount,
        BigDecimal faceValueInPaymentCurrency) {}
