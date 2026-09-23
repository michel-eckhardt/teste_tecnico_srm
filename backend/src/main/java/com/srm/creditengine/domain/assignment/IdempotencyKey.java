package com.srm.creditengine.domain.assignment;

import java.util.Objects;

/**
 * Client-supplied {@code Idempotency-Key} bound to the fingerprint of the request it was first
 * used with. Replaying the key with the same payload returns the original operation; replaying it
 * with a different payload is rejected.
 *
 * @param key value of the {@code Idempotency-Key} header
 * @param requestHash SHA-256 (hex) of the canonical request
 */
public record IdempotencyKey(String key, String requestHash) {

    public IdempotencyKey {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(requestHash, "requestHash");
    }
}
