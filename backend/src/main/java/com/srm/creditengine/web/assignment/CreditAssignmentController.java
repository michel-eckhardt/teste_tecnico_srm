package com.srm.creditengine.web.assignment;

import com.srm.creditengine.domain.assignment.CreationResult;
import com.srm.creditengine.domain.assignment.CreditAssignment;
import com.srm.creditengine.domain.assignment.CreditAssignmentService;
import com.srm.creditengine.domain.assignment.SettlementService;
import com.srm.creditengine.web.support.ApiPaths;
import com.srm.creditengine.web.support.EntityTags;
import com.srm.creditengine.web.support.ProblemResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    private final SettlementService settlements;

    public CreditAssignmentController(CreditAssignmentService creditAssignments, SettlementService settlements) {
        this.creditAssignments = creditAssignments;
        this.settlements = settlements;
    }

    @PostMapping
    @Operation(
            summary = "Cria uma operação de cessão (precifica e persiste o lote atomicamente)",
            description = "201 para uma nova operação. Com Idempotency-Key: a mesma chave com o mesmo conteúdo "
                    + "devolve a operação existente (200); com outro conteúdo, 409 IDEMPOTENCY_KEY_REUSED.")
    @ApiResponse(
            responseCode = "201",
            description = "Operação criada (PENDING)",
            headers = {
                @Header(name = HttpHeaders.LOCATION, description = "URL da operação criada"),
                @Header(name = HttpHeaders.ETAG, description = "Versão da operação, ex.: \"0\"")
            })
    @ApiResponse(
            responseCode = "200",
            description =
                    "Reenvio idempotente: a mesma Idempotency-Key e o mesmo conteúdo devolvem a operação já criada",
            headers = {
                @Header(name = HttpHeaders.CONTENT_LOCATION, description = "URL da operação existente"),
                @Header(name = HttpHeaders.ETAG, description = "Versão atual da operação")
            })
    @ProblemResponses({404, 409, 422})
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
    @ApiResponse(
            responseCode = "200",
            description = "Operação",
            headers = @Header(name = HttpHeaders.ETAG, description = "Versão atual, usada no If-Match"))
    public ResponseEntity<CreditAssignmentResponse> get(@PathVariable UUID id) {
        return withEtag(creditAssignments.get(id));
    }

    @PostMapping("/{id}/settlement")
    @Operation(
            summary = "Liquida a operação (If-Match obrigatório)",
            description = "Na mesma transação: PENDING -> SETTLED, débito da conta-caixa do fundo na moeda de "
                    + "pagamento e registro do movimento. 409 se já liquidada/cancelada ou em conflito concorrente, "
                    + "412 se o If-Match não for a versão atual, 422 INSUFFICIENT_FUNDS, 428 sem If-Match.")
    @ApiResponse(
            responseCode = "200",
            description = "Operação liquidada (SETTLED)",
            headers = @Header(name = HttpHeaders.ETAG, description = "Nova versão da operação"))
    @ProblemResponses({409, 412, 422, 428})
    public ResponseEntity<CreditAssignmentResponse> settle(
            @PathVariable UUID id,
            @Parameter(description = "ETag atual da operação, ex.: \"0\"") @RequestHeader(HttpHeaders.IF_MATCH)
                    String ifMatch) {
        return withEtag(settlements.settle(id, EntityTags.parseIfMatch(ifMatch)));
    }

    @PostMapping("/{id}/cancellation")
    @Operation(summary = "Cancela uma operação pendente (If-Match obrigatório)")
    @ApiResponse(
            responseCode = "200",
            description = "Operação cancelada (CANCELLED)",
            headers = @Header(name = HttpHeaders.ETAG, description = "Nova versão da operação"))
    @ProblemResponses({409, 412, 428})
    public ResponseEntity<CreditAssignmentResponse> cancel(
            @PathVariable UUID id,
            @Parameter(description = "ETag atual da operação, ex.: \"0\"") @RequestHeader(HttpHeaders.IF_MATCH)
                    String ifMatch) {
        return withEtag(settlements.cancel(id, EntityTags.parseIfMatch(ifMatch)));
    }

    private static ResponseEntity<CreditAssignmentResponse> withEtag(CreditAssignment operation) {
        return ResponseEntity.ok()
                .eTag(EntityTags.of(operation.getVersion()))
                .body(CreditAssignmentResponse.from(operation));
    }
}
