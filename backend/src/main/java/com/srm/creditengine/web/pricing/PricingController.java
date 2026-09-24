package com.srm.creditengine.web.pricing;

import com.srm.creditengine.domain.pricing.PricingService;
import com.srm.creditengine.domain.pricing.ReceivableTypeCatalog;
import com.srm.creditengine.web.support.ApiPaths;
import com.srm.creditengine.web.support.ProblemResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1)
@Tag(name = "Precificação", description = "Tipos de recebível e simulação do deságio")
public class PricingController {

    private final PricingService pricingService;
    private final ReceivableTypeCatalog receivableTypes;

    public PricingController(PricingService pricingService, ReceivableTypeCatalog receivableTypes) {
        this.pricingService = pricingService;
        this.receivableTypes = receivableTypes;
    }

    @GetMapping("/receivable-types")
    @Operation(summary = "Tipos de recebível ativos com o spread mensal da Strategy de cada tipo")
    public List<ReceivableTypeResponse> receivableTypes() {
        return receivableTypes.activeTypes().stream()
                .map(ReceivableTypeResponse::from)
                .toList();
    }

    @PostMapping("/pricing/simulations")
    @Operation(
            summary = "Simula a precificação de um recebível (não persiste nada)",
            description = "valorPresente = valorFace / (1 + taxaBase + spread) ^ (prazoDias / 30); conversão cambial "
                    + "aplicada no final sobre o valor não arredondado; arredondamento HALF_EVEN único.")
    @ProblemResponses({422})
    public SimulationResponse simulate(@Valid @RequestBody SimulationRequest request) {
        return SimulationResponse.from(pricingService.simulate(
                request.receivableType(),
                request.faceValue(),
                request.faceCurrency(),
                request.dueDate(),
                request.paymentCurrency()));
    }
}
