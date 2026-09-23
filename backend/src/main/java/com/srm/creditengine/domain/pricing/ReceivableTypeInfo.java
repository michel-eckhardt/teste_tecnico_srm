package com.srm.creditengine.domain.pricing;

import java.math.BigDecimal;

/**
 * Receivable type offered to operators.
 *
 * @param monthlySpread nominal spread of the type, taken from its {@link PricingStrategy}
 */
public record ReceivableTypeInfo(ReceivableType code, String description, BigDecimal monthlySpread) {}
