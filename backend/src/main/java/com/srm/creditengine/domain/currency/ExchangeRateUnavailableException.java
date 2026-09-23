package com.srm.creditengine.domain.currency;

import com.srm.creditengine.domain.common.BusinessException;
import com.srm.creditengine.domain.common.ErrorCode;

/** A cross-currency operation needs a rate that was never registered. */
public class ExchangeRateUnavailableException extends BusinessException {

    public ExchangeRateUnavailableException(CurrencyCode from, CurrencyCode to) {
        super(
                ErrorCode.EXCHANGE_RATE_UNAVAILABLE,
                "Não há taxa de câmbio cadastrada para converter %s em %s. Sincronize ou cadastre uma taxa."
                        .formatted(from, to));
    }
}
