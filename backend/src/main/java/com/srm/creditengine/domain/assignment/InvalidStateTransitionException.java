package com.srm.creditengine.domain.assignment;

import com.srm.creditengine.domain.common.BusinessException;
import com.srm.creditengine.domain.common.ErrorCode;
import java.util.UUID;

/** The requested lifecycle transition is not allowed from the current status. */
public class InvalidStateTransitionException extends BusinessException {

    public InvalidStateTransitionException(
            UUID operationId, CreditAssignmentStatus current, CreditAssignmentStatus target) {
        super(
                ErrorCode.INVALID_STATE_TRANSITION,
                "A operação %s está %s e não pode passar para %s.".formatted(operationId, current, target));
    }
}
