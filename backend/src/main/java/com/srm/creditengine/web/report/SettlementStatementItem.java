package com.srm.creditengine.web.report;

import com.srm.creditengine.domain.assignment.CreditAssignmentStatus;
import com.srm.creditengine.domain.currency.CurrencyCode;
import com.srm.creditengine.persistence.report.SettlementStatementRow;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One line of the settlement statement; totals are in the payment currency. */
public record SettlementStatementItem(
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
        Instant settledAt) {

    static SettlementStatementItem from(SettlementStatementRow row) {
        return new SettlementStatementItem(
                row.operationId(),
                row.assignorId(),
                row.assignorName(),
                row.assignorDocument(),
                row.status(),
                row.paymentCurrency(),
                row.receivablesCount(),
                row.totalFaceValue(),
                row.totalDiscount(),
                row.totalNetAmount(),
                row.createdAt(),
                row.settledAt());
    }
}
