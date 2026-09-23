package com.srm.creditengine.domain.assignment;

import com.srm.creditengine.domain.common.BusinessException;
import com.srm.creditengine.domain.common.ErrorCode;
import java.util.UUID;

/** A settlement was requested for an operation that has already been paid. */
public class OperationAlreadySettledException extends BusinessException {

    public OperationAlreadySettledException(UUID operationId) {
        super(ErrorCode.OPERATION_ALREADY_SETTLED, "A operação %s já foi liquidada.".formatted(operationId));
    }
}
