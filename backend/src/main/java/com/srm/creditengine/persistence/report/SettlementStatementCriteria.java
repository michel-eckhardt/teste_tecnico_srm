package com.srm.creditengine.persistence.report;

import com.srm.creditengine.domain.assignment.CreditAssignmentStatus;
import com.srm.creditengine.domain.currency.CurrencyCode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Filters of the settlement statement; {@code null} means "no filter".
 *
 * @param createdFrom inclusive lower bound of the operation creation instant
 * @param createdBefore exclusive upper bound of the operation creation instant
 */
public record SettlementStatementCriteria(
        @Nullable Instant createdFrom,
        @Nullable Instant createdBefore,
        @Nullable UUID assignorId,
        @Nullable CurrencyCode currency,
        @Nullable CreditAssignmentStatus status,
        SettlementStatementSort sort,
        int page,
        int size) {

    public SettlementStatementCriteria {
        Objects.requireNonNull(sort, "sort");
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("page must be >= 0 and size >= 1");
        }
    }

    long offset() {
        return (long) page * size;
    }
}
