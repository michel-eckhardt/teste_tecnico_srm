package com.srm.creditengine.domain.common;

/**
 * The client based its command on an outdated version of the resource (optimistic concurrency
 * control through {@code If-Match}).
 */
public class StaleVersionException extends BusinessException {

    public StaleVersionException(long expectedVersion, long currentVersion) {
        super(
                ErrorCode.PRECONDITION_FAILED,
                "A versão informada (%d) não corresponde à versão atual (%d) do recurso. Recarregue-o e tente novamente."
                        .formatted(expectedVersion, currentVersion));
    }
}
