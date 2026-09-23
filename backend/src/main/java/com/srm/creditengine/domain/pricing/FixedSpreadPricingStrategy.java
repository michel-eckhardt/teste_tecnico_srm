package com.srm.creditengine.domain.pricing;

import java.math.BigDecimal;
import java.util.Objects;

/** Base for strategies whose spread does not depend on the individual receivable. */
public abstract class FixedSpreadPricingStrategy implements PricingStrategy {

    /** Rates are handled (and exposed) with 8 decimal places. */
    public static final int RATE_SCALE = 8;

    private final ReceivableType type;
    private final BigDecimal monthlySpread;

    protected FixedSpreadPricingStrategy(ReceivableType type, BigDecimal monthlySpread) {
        this.type = Objects.requireNonNull(type, "type");
        Objects.requireNonNull(monthlySpread, "monthlySpread");
        if (monthlySpread.signum() < 0 || monthlySpread.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException(
                    "monthly spread of %s must be in [0, 1): %s".formatted(type, monthlySpread));
        }
        this.monthlySpread = monthlySpread.setScale(RATE_SCALE);
    }

    @Override
    public final ReceivableType type() {
        return type;
    }

    @Override
    public BigDecimal monthlySpread(PricingContext context) {
        return monthlySpread;
    }

    @Override
    public BigDecimal nominalMonthlySpread() {
        return monthlySpread;
    }
}
