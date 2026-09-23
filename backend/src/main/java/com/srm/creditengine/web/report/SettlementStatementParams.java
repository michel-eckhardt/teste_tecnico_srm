package com.srm.creditengine.web.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.srm.creditengine.domain.assignment.CreditAssignmentStatus;
import com.srm.creditengine.domain.currency.CurrencyCode;
import com.srm.creditengine.persistence.report.SettlementStatementSort;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Query parameters of the settlement statement. Every filter is optional.
 *
 * @param from first business date (inclusive) of the operation creation period
 * @param to last business date (inclusive) of the operation creation period
 * @param sort {@code field,direction} among the whitelisted fields
 */
public record SettlementStatementParams(
        @Schema(example = "2026-09-01") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable
        LocalDate from,

        @Schema(example = "2026-09-30") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable
        LocalDate to,

        @Nullable UUID assignorId,
        @Nullable CurrencyCode currency,
        @Nullable CreditAssignmentStatus status,
        @Schema(defaultValue = "0") @Min(0) @Nullable Integer page,

        @Schema(defaultValue = "20") @Min(1) @Max(100) @Nullable
        Integer size,

        @Schema(
                defaultValue = "createdAt,desc",
                description = "Campos: createdAt, settledAt, totalNetAmount, assignorName; direção asc|desc")
        @Pattern(
                regexp = SORT_PATTERN,
                message = "deve ser campo[,asc|desc] com campo em createdAt, settledAt, totalNetAmount ou assignorName")
        @Nullable
        String sort) {

    static final String SORT_PATTERN = "^(createdAt|settledAt|totalNetAmount|assignorName)(,(asc|desc|ASC|DESC))?$";

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "a data inicial (from) deve ser anterior ou igual à data final (to)")
    public boolean isPeriodValid() {
        return from == null || to == null || !from.isAfter(to);
    }

    int pageOrDefault() {
        return page == null ? 0 : page;
    }

    int sizeOrDefault() {
        return size == null ? 20 : size;
    }

    SettlementStatementSort sortOrDefault() {
        return sort == null
                ? SettlementStatementSort.DEFAULT
                : SettlementStatementSort.parse(sort)
                        .orElseThrow(() -> new IllegalArgumentException("unvalidated sort"));
    }
}
