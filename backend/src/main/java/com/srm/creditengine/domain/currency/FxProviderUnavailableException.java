package com.srm.creditengine.domain.currency;

import com.srm.creditengine.domain.common.BusinessException;
import com.srm.creditengine.domain.common.ErrorCode;

/**
 * The external exchange rate provider could not deliver usable rates. Callers keep working with
 * the last persisted rate until it expires.
 */
public class FxProviderUnavailableException extends BusinessException {

    public FxProviderUnavailableException(String reason, Throwable cause) {
        super(
                ErrorCode.FX_PROVIDER_UNAVAILABLE,
                "O provedor de câmbio está indisponível (%s). As últimas taxas persistidas continuam em uso."
                        .formatted(reason),
                cause);
    }
}
