package com.srm.creditengine.web.assignment;

import com.srm.creditengine.domain.assignment.CreditAssignment;
import com.srm.creditengine.domain.assignment.NewCreditAssignment;
import com.srm.creditengine.domain.currency.CurrencyCode;
import com.srm.creditengine.web.pricing.ReceivableLimits;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Batch of receivables bought from one assignor and paid in one currency. */
public record CreateCreditAssignmentRequest(
        @NotNull UUID assignorId,
        @NotNull @Schema(example = "BRL") CurrencyCode paymentCurrency,

        @NotNull @Size(min = 1, max = CreditAssignment.MAX_RECEIVABLES)
        List<@NotNull @Valid ReceivableRequest> receivables) {

    public record ReceivableRequest(
            @NotBlank @Size(max = 40) @Schema(example = "DUPLICATA_MERCANTIL")
            String receivableType,

            @NotNull
            @Positive
            @Digits(integer = 13, fraction = 2)
            @DecimalMax(ReceivableLimits.MAX_FACE_VALUE)
            @Schema(example = "10000.00")
            BigDecimal faceValue,

            @NotNull @Schema(example = "BRL") CurrencyCode faceCurrency,
            @NotNull @Schema(example = "2026-12-22") LocalDate dueDate) {}

    NewCreditAssignment toCommand() {
        return new NewCreditAssignment(
                assignorId,
                paymentCurrency,
                receivables.stream()
                        .map(item -> new NewCreditAssignment.Item(
                                item.receivableType(), item.faceValue(), item.faceCurrency(), item.dueDate()))
                        .toList());
    }
}
