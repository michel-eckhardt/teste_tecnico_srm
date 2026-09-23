package com.srm.creditengine.web.assignor;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.support.Cnpjs;
import com.srm.creditengine.support.IntegrationTest;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@IntegrationTest
class AssignorApiIT {

    @Autowired
    private RestTestClient client;

    private RestTestClient.ResponseSpec register(String name, String document) {
        return client.post()
                .uri("/api/v1/assignors")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"%s\",\"document\":\"%s\"}".formatted(name, document))
                .exchange();
    }

    private static String formatted(String cnpj) {
        return "%s.%s.%s/%s-%s"
                .formatted(
                        cnpj.substring(0, 2),
                        cnpj.substring(2, 5),
                        cnpj.substring(5, 8),
                        cnpj.substring(8, 12),
                        cnpj.substring(12));
    }

    @Test
    void registersAnAssignorWithAFormattedCnpjStoredAsDigits() {
        String cnpj = Cnpjs.random();

        URI location = register("Fábrica Formatada Ltda", formatted(cnpj))
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.document")
                .isEqualTo(cnpj)
                .jsonPath("$.name")
                .isEqualTo("Fábrica Formatada Ltda")
                .returnResult()
                .getResponseHeaders()
                .getLocation();

        client.get()
                .uri(location)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.document")
                .isEqualTo(cnpj);
    }

    @Test
    void rejectsAnInvalidCnpj() {
        register("Empresa Inválida", "11222333000182")
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.errors[0].field")
                .isEqualTo("document")
                .jsonPath("$.errors[0].message")
                .value(message -> assertThat((String) message).contains("CNPJ"));
    }

    @Test
    void rejectsADuplicateCnpj() {
        String cnpj = Cnpjs.random();
        register("Primeira", cnpj).expectStatus().isCreated();

        register("Segunda", formatted(cnpj))
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("DUPLICATE_ASSIGNOR");
    }

    @Test
    void searchesByNameFragmentAndByCnpjPrefix() {
        String cnpj = Cnpjs.random();
        String marker = "Zeta" + UUID.randomUUID().toString().substring(0, 8);
        register(marker + " Comércio S.A.", cnpj).expectStatus().isCreated();

        client.get()
                .uri("/api/v1/assignors?search={term}&size=5", marker.toLowerCase())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.content[*].document")
                .isEqualTo(List.of(cnpj))
                .jsonPath("$.page.size")
                .isEqualTo(5)
                .jsonPath("$.page.totalElements")
                .isEqualTo(1);

        client.get()
                .uri("/api/v1/assignors?search={term}", formatted(cnpj))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.content[0].document")
                .isEqualTo(cnpj);
    }

    @Test
    void unknownAssignorIsNotFound() {
        client.get()
                .uri("/api/v1/assignors/{id}", UUID.randomUUID())
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }
}
