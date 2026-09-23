package com.srm.creditengine.web.currency;

import com.srm.creditengine.domain.currency.ExchangeRateService;
import com.srm.creditengine.web.support.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/currencies")
@Tag(name = "Câmbio", description = "Moedas e taxas de câmbio")
public class CurrencyController {

    private final ExchangeRateService exchangeRateService;

    public CurrencyController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping
    @Operation(summary = "Lista as moedas suportadas")
    public List<CurrencyResponse> list() {
        return exchangeRateService.currencies().stream()
                .map(CurrencyResponse::from)
                .toList();
    }
}
