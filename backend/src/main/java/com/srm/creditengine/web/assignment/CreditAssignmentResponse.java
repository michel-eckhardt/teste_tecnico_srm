package com.srm.creditengine.web.assignment;

import com.srm.creditengine.domain.assignment.CreditAssignment;
import com.srm.creditengine.domain.assignment.CreditAssignmentStatus;
import com.srm.creditengine.domain.assignment.Receivable;
import com.srm.creditengine.domain.assignor.Assignor;
import com.srm.creditengine.domain.currency.CurrencyCode;
import com.srm.creditengine.domain.currency.ExchangeRate;
import com.srm.creditengine.domain.pricing.ReceivableType;
import com.srm.creditengine.web.pricing.ExchangeRateSnapshot;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Credit assignment operation; totals are expressed in the payment currency. */
public record CreditAssignmentResponse(
        UUID id,
        CreditAssignmentStatus status,

        @Schema(description = "Versão para controle otimista (também devolvida no ETag)")
        long version,

        AssignorSummary assignor,
        CurrencyCode paymentCurrency,
        BigDecimal totalFaceValue,
        BigDecimal totalDiscount,
        BigDecimal totalNetAmount,
        int receivablesCount,
        List<ReceivableResponse> receivables,
        Instant createdAt,
        Instant settledAt,
        Instant cancelledAt) {

    public record AssignorSummary(UUID id, String name, String document) {}

    public record ReceivableResponse(
            UUID id,
            ReceivableType receivableType,
            BigDecimal faceValue,
            CurrencyCode faceCurrency,
            LocalDate dueDate,
            int termDays,
            BigDecimal baseRate,
            BigDecimal spread,
            @Schema(description = "Na moeda de face") BigDecimal presentValue,
            @Schema(description = "Na moeda de face") BigDecimal discount,

            @Schema(description = "null quando a moeda de face é a de pagamento")
            ExchangeRateSnapshot exchangeRate,

            @Schema(description = "Na moeda de pagamento") BigDecimal netAmount) {

        static ReceivableResponse from(Receivable receivable) {
            ExchangeRate rate = receivable.getExchangeRate();
            ExchangeRateSnapshot snapshot = rate == null
                    ? null
                    : new ExchangeRateSnapshot(
                            rate.getId(),
                            rate.getBaseCurrency(),
                            rate.getQuoteCurrency(),
                            receivable.getExchangeRateApplied(),
                            rate.getReferenceDate());
            return new ReceivableResponse(
                    receivable.getId(),
                    receivable.getType(),
                    receivable.getFaceValue(),
                    receivable.getFaceCurrency(),
                    receivable.getDueDate(),
                    receivable.getTermDays(),
                    receivable.getBaseRate(),
                    receivable.getSpread(),
                    receivable.getPresentValue(),
                    receivable.getDiscount(),
                    snapshot,
                    receivable.getNetAmount());
        }
    }

    public static CreditAssignmentResponse from(CreditAssignment operation) {
        Assignor assignor = operation.getAssignor();
        return new CreditAssignmentResponse(
                operation.getId(),
                operation.getStatus(),
                operation.getVersion(),
                new AssignorSummary(assignor.getId(), assignor.getName(), assignor.getDocument()),
                operation.getPaymentCurrency(),
                operation.getTotalFaceValue(),
                operation.getTotalDiscount(),
                operation.getTotalNetAmount(),
                operation.getReceivablesCount(),
                operation.getReceivables().stream()
                        .map(ReceivableResponse::from)
                        .toList(),
                operation.getCreatedAt(),
                operation.getSettledAt(),
                operation.getCancelledAt());
    }
}
