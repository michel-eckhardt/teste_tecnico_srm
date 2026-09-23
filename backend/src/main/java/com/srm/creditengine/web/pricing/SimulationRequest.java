package com.srm.creditengine.web.pricing;

import com.srm.creditengine.domain.currency.CurrencyCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pricing simulation of one receivable. Business rules (type accepted, due date after today and
 * within the maximum term, fresh FX rate) are checked by the domain and answered with 422.
 */
public record SimulationRequest(
        @NotBlank @Size(max = 40) @Schema(example = "DUPLICATA_MERCANTIL")
        String receivableType,

        @NotNull
        @Positive
        @Digits(integer = 13, fraction = 2)
        @DecimalMax(ReceivableLimits.MAX_FACE_VALUE)
        @Schema(example = "10000.00")
        BigDecimal faceValue,

        @NotNull @Schema(example = "BRL") CurrencyCode faceCurrency,
        @NotNull @Schema(example = "2026-12-22") LocalDate dueDate,
        @NotNull @Schema(example = "USD") CurrencyCode paymentCurrency) {}
