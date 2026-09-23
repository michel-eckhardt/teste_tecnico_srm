package com.srm.creditengine.domain.common;

/**
 * Stable, client-facing identifiers of business errors. The application layer translates each
 * code into an HTTP status and a problem type; clients branch on the code, never on the message.
 */
public enum ErrorCode {
    RESOURCE_NOT_FOUND,
    EXCHANGE_RATE_UNAVAILABLE,
    EXCHANGE_RATE_STALE,
    FX_PROVIDER_UNAVAILABLE,
    INVALID_DUE_DATE,
    UNSUPPORTED_RECEIVABLE_TYPE
}
