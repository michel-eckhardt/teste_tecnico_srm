package com.srm.creditengine.domain.pricing;

import java.math.BigDecimal;

/**
 * Risk rule of one receivable type (Strategy pattern). The pricing engine is closed for
 * modification: supporting a new receivable type means adding a new implementation (and its row in
 * {@code receivable_type}), never editing the engine.
 */
public interface PricingStrategy {

    /** Receivable type this strategy prices; each type must have exactly one strategy. */
    ReceivableType type();

    /** Monthly risk spread (decimal fraction, e.g. {@code 0.015} = 1.5% a.m.) for this receivable. */
    BigDecimal monthlySpread(PricingContext context);

    /** Spread advertised in catalogs (the spread of a typical receivable of this type). */
    BigDecimal nominalMonthlySpread();
}
