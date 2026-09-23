package com.srm.creditengine.domain.pricing;

import java.util.Arrays;
import java.util.Optional;

/**
 * Receivable products bought by the fund. Each type is priced by exactly one {@link PricingStrategy}
 * and must exist in the {@code receivable_type} table (description and activation flag).
 */
public enum ReceivableType {
    DUPLICATA_MERCANTIL,
    CHEQUE_PRE_DATADO;

    /** Case-sensitive lookup by code, without exceptions for unknown values. */
    public static Optional<ReceivableType> fromCode(String code) {
        return Arrays.stream(values()).filter(type -> type.name().equals(code)).findFirst();
    }
}
