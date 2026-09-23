package com.srm.creditengine.domain.pricing;

import com.srm.creditengine.domain.currency.CurrencyCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** Contractual terms of a receivable to be priced. */
public record ReceivableTerms(ReceivableType type, BigDecimal faceValue, CurrencyCode faceCurrency, LocalDate dueDate) {

    public ReceivableTerms {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(faceValue, "faceValue");
        Objects.requireNonNull(faceCurrency, "faceCurrency");
        Objects.requireNonNull(dueDate, "dueDate");
        if (faceValue.signum() <= 0) {
            throw new IllegalArgumentException("face value must be positive");
        }
    }
}
