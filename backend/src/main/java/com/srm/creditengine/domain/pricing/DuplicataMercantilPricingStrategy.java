package com.srm.creditengine.domain.pricing;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Duplicata mercantil: trade receivable backed by an invoice of goods actually delivered, the lower
 * risk product of the fund. Default spread: 1.5% a.m. ({@code srm.pricing.spreads.duplicata-mercantil}).
 */
@Component
public class DuplicataMercantilPricingStrategy extends FixedSpreadPricingStrategy {

    public DuplicataMercantilPricingStrategy(
            @Value("${srm.pricing.spreads.duplicata-mercantil:0.015}") BigDecimal monthlySpread) {
        super(ReceivableType.DUPLICATA_MERCANTIL, monthlySpread);
    }
}
