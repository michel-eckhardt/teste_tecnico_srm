package com.srm.creditengine.persistence.report;

import com.srm.creditengine.domain.assignment.CreditAssignmentStatus;
import com.srm.creditengine.domain.currency.CurrencyCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** One line of the settlement statement, read straight from SQL (no entity involved). */
public record SettlementStatementRow(
        UUID operationId,
        UUID assignorId,
        String assignorName,
        String assignorDocument,
        CreditAssignmentStatus status,
        CurrencyCode paymentCurrency,
        int receivablesCount,
        BigDecimal totalFaceValue,
        BigDecimal totalDiscount,
        BigDecimal totalNetAmount,
        Instant createdAt,
        @Nullable Instant settledAt) {}
