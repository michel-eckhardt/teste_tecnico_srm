package com.srm.creditengine.domain.assignment;

import com.srm.creditengine.domain.common.BusinessException;
import com.srm.creditengine.domain.common.ErrorCode;

/** The {@code Idempotency-Key} was already used with a different request payload. */
public class IdempotencyKeyReusedException extends BusinessException {

    public IdempotencyKeyReusedException(String key) {
        super(
                ErrorCode.IDEMPOTENCY_KEY_REUSED,
                "A chave de idempotência '%s' já foi usada com outro conteúdo. Gere uma nova chave para uma nova operação."
                        .formatted(key));
    }
}
