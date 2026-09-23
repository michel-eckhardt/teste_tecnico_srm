package com.srm.creditengine.web.currency;

import com.srm.creditengine.domain.currency.ExchangeRateService;
import com.srm.creditengine.domain.currency.ExchangeRateSyncService;
import com.srm.creditengine.domain.currency.ExchangeRateView;
import com.srm.creditengine.web.support.ApiPaths;
import com.srm.creditengine.web.support.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(ApiPaths.V1 + "/exchange-rates")
@Tag(name = "Câmbio", description = "Moedas e taxas de câmbio")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;
    private final ExchangeRateSyncService exchangeRateSyncService;

    public ExchangeRateController(
            ExchangeRateService exchangeRateService, ExchangeRateSyncService exchangeRateSyncService) {
        this.exchangeRateService = exchangeRateService;
        this.exchangeRateSyncService = exchangeRateSyncService;
    }

    @GetMapping("/latest")
    @Operation(
            summary = "Taxa vigente de um par",
            description = "Usa o par publicado ou o inverso derivado, o que for mais recente. 404 se não houver taxa.")
    public ExchangeRateResponse latest(@Valid @ParameterObject CurrencyPairQuery pair) {
        return ExchangeRateResponse.from(exchangeRateService.latest(pair.base(), pair.quote()));
    }

    @GetMapping
    @Operation(summary = "Histórico de taxas de um par (mais recente primeiro)")
    public PageResponse<ExchangeRateResponse> history(
            @Valid @ParameterObject CurrencyPairQuery pair,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(
                exchangeRateService.history(pair.base(), pair.quote(), page, size), ExchangeRateResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma taxa pelo identificador")
    public ExchangeRateResponse get(@PathVariable UUID id) {
        return ExchangeRateResponse.from(exchangeRateService.get(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra uma taxa manualmente")
    public ResponseEntity<ExchangeRateResponse> register(@Valid @RequestBody ManualExchangeRateRequest request) {
        ExchangeRateView created = exchangeRateService.registerManual(
                request.base(), request.quote(), request.rate(), request.referenceDate());
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(ExchangeRateResponse.from(created));
    }

    @PostMapping("/sync")
    @Operation(
            summary = "Sincroniza as taxas com a API Frankfurter",
            description = "Chamada protegida por retry e circuit breaker. Idempotente: taxas idênticas não são "
                    + "duplicadas. 503 FX_PROVIDER_UNAVAILABLE se o provedor estiver indisponível.")
    public List<ExchangeRateResponse> synchronize() {
        return exchangeRateSyncService.synchronize().stream()
                .map(ExchangeRateResponse::from)
                .toList();
    }
}
