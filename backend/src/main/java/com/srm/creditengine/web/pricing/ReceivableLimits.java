package com.srm.creditengine.web.pricing;

/** Input limits shared by every request that carries receivables. */
public final class ReceivableLimits {

    /** Largest face value accepted for a single receivable. */
    public static final String MAX_FACE_VALUE = "1000000000.00";

    private ReceivableLimits() {}
}
