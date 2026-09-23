package com.srm.creditengine.web.pricing;

import com.srm.creditengine.domain.pricing.ReceivableType;
import com.srm.creditengine.domain.pricing.ReceivableTypeInfo;
import java.math.BigDecimal;

public record ReceivableTypeResponse(ReceivableType code, String description, BigDecimal monthlySpread) {

    static ReceivableTypeResponse from(ReceivableTypeInfo info) {
        return new ReceivableTypeResponse(info.code(), info.description(), info.monthlySpread());
    }
}
