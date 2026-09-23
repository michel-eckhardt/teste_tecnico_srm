package com.srm.creditengine.domain.pricing;

import com.srm.creditengine.domain.common.BusinessException;
import com.srm.creditengine.domain.common.ErrorCode;

/** The receivable type is unknown or no longer accepted for new operations. */
public class UnsupportedReceivableTypeException extends BusinessException {

    public UnsupportedReceivableTypeException(String code) {
        super(
                ErrorCode.UNSUPPORTED_RECEIVABLE_TYPE,
                "Tipo de recebível '%s' não é suportado ou está inativo.".formatted(code));
    }
}
