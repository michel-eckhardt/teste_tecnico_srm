package com.srm.creditengine.web.pricing;

import com.srm.creditengine.domain.currency.CurrencyCode;
import com.srm.creditengine.domain.pricing.PricedReceivable;
import com.srm.creditengine.domain.pricing.ReceivableType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SimulationResponse(
        ReceivableType receivableType,
        BigDecimal faceValue,
        CurrencyCode faceCurrency,
        CurrencyCode paymentCurrency,
        LocalDate operationDate,
        LocalDate dueDate,
        int termDays,
        @Schema(description = "termDays / 30") BigDecimal termMonths,
        BigDecimal baseRate,
        BigDecimal spread,
        @Schema(description = "baseRate + spread (mensal)") BigDecimal discountRate,
        @Schema(description = "Na moeda de face") BigDecimal presentValue,
        @Schema(description = "Na moeda de face") BigDecimal discount,

        @Schema(description = "null quando faceCurrency == paymentCurrency")
        ExchangeRateSnapshot exchangeRate,

        @Schema(description = "Valor presente na moeda de pagamento")
        BigDecimal netAmount) {

    static SimulationResponse from(PricedReceivable priced) {
        return new SimulationResponse(
                priced.type(),
                priced.faceValue(),
                priced.faceCurrency(),
                priced.paymentCurrency(),
                priced.operationDate(),
                priced.dueDate(),
                priced.termDays(),
                priced.termMonths(),
                priced.baseRate(),
                priced.spread(),
                priced.discountRate(),
                priced.presentValue(),
                priced.discount(),
                ExchangeRateSnapshot.from(priced.conversion()),
                priced.netAmount());
    }
}
