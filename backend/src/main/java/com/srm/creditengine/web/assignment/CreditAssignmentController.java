package com.srm.creditengine.web.assignment;

import com.srm.creditengine.domain.assignment.CreationResult;
import com.srm.creditengine.domain.assignment.CreditAssignment;
import com.srm.creditengine.domain.assignment.CreditAssignmentService;
import com.srm.creditengine.web.support.ApiPaths;
import com.srm.creditengine.web.support.EntityTags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(ApiPaths.V1 + "/credit-assignments")
@Tag(name = "Operações de cessão", description = "Cessão de lotes de recebíveis e liquidação")
public class CreditAssignmentController {

    static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final CreditAssignmentService creditAssignments;

    public CreditAssignmentController(CreditAssignmentService creditAssignments) {
        this.creditAssignments = creditAssignments;
    }

    @PostMapping
    @Operation(
            summary = "Cria uma operação de cessão (precifica e persiste o lote atomicamente)",
            description = "201 para uma nova operação. Com Idempotency-Key: a mesma chave com o mesmo conteúdo "
                    + "devolve a operação existente (200); com outro conteúdo, 409 IDEMPOTENCY_KEY_REUSED.")
    public ResponseEntity<CreditAssignmentResponse> create(
            @Parameter(description = "Chave de idempotência (UUID recomendado)")
                    @RequestHeader(name = IDEMPOTENCY_KEY, required = false)
                    @Pattern(regexp = "[A-Za-z0-9._:-]{1,100}")
                    String idempotencyKey,
            @Valid @RequestBody CreateCreditAssignmentRequest request) {
        CreationResult result = creditAssignments.create(request.toCommand(), idempotencyKey);
        CreditAssignment operation = result.assignment();
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(operation.getId())
                .toUri();
        ResponseEntity.BodyBuilder response = result.created()
                ? ResponseEntity.status(HttpStatus.CREATED).location(location)
                : ResponseEntity.ok().header(HttpHeaders.CONTENT_LOCATION, location.toString());
        return response.eTag(EntityTags.of(operation.getVersion())).body(CreditAssignmentResponse.from(operation));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma operação (ETag = versão atual)")
    public ResponseEntity<CreditAssignmentResponse> get(@PathVariable UUID id) {
        CreditAssignment operation = creditAssignments.get(id);
        return ResponseEntity.ok()
                .eTag(EntityTags.of(operation.getVersion()))
                .body(CreditAssignmentResponse.from(operation));
    }
}
