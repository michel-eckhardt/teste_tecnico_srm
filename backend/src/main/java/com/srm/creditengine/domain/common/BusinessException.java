package com.srm.creditengine.domain.common;

import java.util.Objects;

/**
 * Violation of a business rule. The message is the user-facing detail (in Portuguese) and must
 * never contain sensitive data.
 */
public abstract class BusinessException extends RuntimeException {

    private final ErrorCode code;

    protected BusinessException(ErrorCode code, String message) {
        this(code, message, null);
    }

    protected BusinessException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
    }

    public ErrorCode code() {
        return code;
    }
}
