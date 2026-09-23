package com.srm.creditengine.domain.currency;

import com.srm.creditengine.domain.common.BusinessException;
import com.srm.creditengine.domain.common.ErrorCode;
import java.time.Duration;

/** The most recent rate of a pair is too old to price an operation safely. */
public class ExchangeRateStaleException extends BusinessException {

    public ExchangeRateStaleException(ExchangeRate rate, Duration maxAge) {
        super(
                ErrorCode.EXCHANGE_RATE_STALE,
                "A taxa de câmbio %s/%s mais recente é de %s e excede a idade máxima permitida (%s). Sincronize ou cadastre uma nova taxa."
                        .formatted(
                                rate.getBaseCurrency(),
                                rate.getQuoteCurrency(),
                                rate.getReferenceDate(),
                                describe(maxAge)));
    }

    private static String describe(Duration maxAge) {
        return maxAge.toHoursPart() == 0 && maxAge.toMinutesPart() == 0
                ? maxAge.toDays() + " dia(s)"
                : maxAge.toHours() + " hora(s)";
    }
}
