package com.srm.creditengine.domain.pricing;

import com.srm.creditengine.domain.common.BusinessException;
import com.srm.creditengine.domain.common.ErrorCode;

/** The due date is not after the operation date or exceeds the maximum term. */
public class InvalidDueDateException extends BusinessException {

    public InvalidDueDateException(String detail) {
        super(ErrorCode.INVALID_DUE_DATE, detail);
    }
}
