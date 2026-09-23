package com.srm.creditengine.web.assignor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CNPJ;

/**
 * Assignor registration. The CNPJ may be sent formatted ({@code 11.222.333/0001-81}) or not; it is
 * normalized to digits only before validation and storage.
 */
public record AssignorRequest(
        @NotBlank @Size(max = 150) @Schema(example = "ACME Indústria Ltda")
        String name,

        @NotBlank @CNPJ @Schema(example = "11222333000181") String document) {

    public AssignorRequest {
        document = document == null ? null : document.replaceAll("[\\s./-]", "");
    }
}
