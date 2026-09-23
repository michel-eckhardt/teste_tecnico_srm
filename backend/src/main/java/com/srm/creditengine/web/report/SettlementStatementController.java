package com.srm.creditengine.web.report;

import com.srm.creditengine.domain.common.BusinessClock;
import com.srm.creditengine.persistence.report.SettlementStatementCriteria;
import com.srm.creditengine.persistence.report.SettlementStatementPage;
import com.srm.creditengine.persistence.report.SettlementStatementRepository;
import com.srm.creditengine.web.support.ApiPaths;
import com.srm.creditengine.web.support.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Analytical report in two layers on purpose (allowed by the architecture rules): the controller
 * talks straight to a native SQL query, without the business layer, because nothing here is a
 * business rule and the ORM would only add overhead on large volumes.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/reports/settlement-statement")
@Tag(name = "Relatórios", description = "Consultas analíticas")
public class SettlementStatementController {

    private final SettlementStatementRepository statements;
    private final BusinessClock clock;

    public SettlementStatementController(SettlementStatementRepository statements, BusinessClock clock) {
        this.statements = statements;
        this.clock = clock;
    }

    @GetMapping
    @Operation(
            summary = "Extrato de liquidação / histórico de operações",
            description = "Filtros opcionais por período (datas de negócio, inclusivas), cedente, moeda de pagamento e "
                    + "status; paginação no servidor; ordenação por whitelist (createdAt, settledAt, totalNetAmount, "
                    + "assignorName).")
    public PageResponse<SettlementStatementItem> statement(@Valid @ParameterObject SettlementStatementParams params) {
        // Business dates become a half-open instant range, so created_at is compared as-is (index friendly).
        SettlementStatementCriteria criteria = new SettlementStatementCriteria(
                params.from() == null ? null : clock.startOf(params.from()),
                params.to() == null ? null : clock.startOf(params.to().plusDays(1)),
                params.assignorId(),
                params.currency(),
                params.status(),
                params.sortOrDefault(),
                params.pageOrDefault(),
                params.sizeOrDefault());
        SettlementStatementPage page = statements.find(criteria);
        return PageResponse.of(
                page.content().stream().map(SettlementStatementItem::from).toList(),
                criteria.page(),
                criteria.size(),
                page.totalElements());
    }
}
