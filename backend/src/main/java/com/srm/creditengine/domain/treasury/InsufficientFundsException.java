package com.srm.creditengine.domain.treasury;

import com.srm.creditengine.domain.common.BusinessException;
import com.srm.creditengine.domain.common.ErrorCode;
import com.srm.creditengine.domain.currency.CurrencyCode;
import java.math.BigDecimal;

/** The fund cash account cannot pay for the operation. */
public class InsufficientFundsException extends BusinessException {

    public InsufficientFundsException(CurrencyCode currency, BigDecimal required, BigDecimal available) {
        super(
                ErrorCode.INSUFFICIENT_FUNDS,
                "Saldo em %s insuficiente para liquidar a operação: necessário %s, disponível %s."
                        .formatted(currency, required.toPlainString(), available.toPlainString()));
    }
}
