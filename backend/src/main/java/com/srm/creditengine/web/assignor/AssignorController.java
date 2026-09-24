package com.srm.creditengine.web.assignor;

import com.srm.creditengine.domain.assignor.Assignor;
import com.srm.creditengine.domain.assignor.AssignorService;
import com.srm.creditengine.web.support.ApiPaths;
import com.srm.creditengine.web.support.PageResponse;
import com.srm.creditengine.web.support.ProblemResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
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
@RequestMapping(ApiPaths.V1 + "/assignors")
@Tag(name = "Cedentes", description = "Empresas que cedem recebíveis ao fundo")
public class AssignorController {

    private final AssignorService assignorService;

    public AssignorController(AssignorService assignorService) {
        this.assignorService = assignorService;
    }

    @PostMapping
    @Operation(summary = "Cadastra um cedente (CNPJ único)")
    @ApiResponse(
            responseCode = "201",
            description = "Cedente cadastrado",
            headers = @Header(name = HttpHeaders.LOCATION, description = "URL do cedente criado"))
    @ProblemResponses({409})
    public ResponseEntity<AssignorResponse> register(@Valid @RequestBody AssignorRequest request) {
        Assignor assignor = assignorService.register(request.name(), request.document());
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(assignor.getId())
                .toUri();
        return ResponseEntity.created(location).body(AssignorResponse.from(assignor));
    }

    @GetMapping
    @Operation(summary = "Pesquisa cedentes por nome ou início do CNPJ")
    public PageResponse<AssignorResponse> search(
            @RequestParam(required = false) @Size(max = 150) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(assignorService.search(search, page, size), AssignorResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um cedente")
    public AssignorResponse get(@PathVariable UUID id) {
        return AssignorResponse.from(assignorService.get(id));
    }
}
