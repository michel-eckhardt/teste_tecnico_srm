package com.srm.creditengine.web.currency;

import com.srm.creditengine.domain.currency.Currency;
import com.srm.creditengine.domain.currency.CurrencyCode;

public record CurrencyResponse(CurrencyCode code, String name, int decimals) {

    static CurrencyResponse from(Currency currency) {
        return new CurrencyResponse(currency.getCode(), currency.getName(), currency.getDecimals());
    }
}
