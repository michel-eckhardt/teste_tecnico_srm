package com.srm.creditengine.web.treasury;

import com.srm.creditengine.domain.currency.CurrencyCode;
import com.srm.creditengine.domain.treasury.FundCashAccount;
import com.srm.creditengine.domain.treasury.TreasuryService;
import com.srm.creditengine.web.support.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/cash-accounts")
@Tag(name = "Contas-caixa", description = "Saldos do fundo por moeda")
public class CashAccountController {

    public record CashAccountResponse(CurrencyCode currency, BigDecimal balance, Instant updatedAt) {

        static CashAccountResponse from(FundCashAccount account) {
            return new CashAccountResponse(account.getCurrency(), account.getBalance(), account.getUpdatedAt());
        }
    }

    private final TreasuryService treasury;

    public CashAccountController(TreasuryService treasury) {
        this.treasury = treasury;
    }

    @GetMapping
    @Operation(summary = "Saldo das contas-caixa do fundo")
    public List<CashAccountResponse> list() {
        return treasury.accounts().stream().map(CashAccountResponse::from).toList();
    }
}
