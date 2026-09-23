package com.srm.creditengine.domain.pricing;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cheque pré-datado: post-dated check, exposed to insufficient funds and countermand, hence a
 * higher spread. Default: 2.5% a.m. ({@code srm.pricing.spreads.cheque-pre-datado}).
 */
@Component
public class ChequePreDatadoPricingStrategy extends FixedSpreadPricingStrategy {

    public ChequePreDatadoPricingStrategy(
            @Value("${srm.pricing.spreads.cheque-pre-datado:0.025}") BigDecimal monthlySpread) {
        super(ReceivableType.CHEQUE_PRE_DATADO, monthlySpread);
    }
}
