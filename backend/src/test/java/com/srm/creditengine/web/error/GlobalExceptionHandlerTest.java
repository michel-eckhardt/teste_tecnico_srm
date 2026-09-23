package com.srm.creditengine.web.error;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srm.creditengine.domain.common.ResourceNotFoundException;
import com.srm.creditengine.web.support.CorrelationIdFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.ProbeController.class)
@Import(GlobalExceptionHandlerTest.ProbeController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    enum Color {
        RED,
        GREEN
    }

    record ProbeRequest(@NotBlank String name, @Positive BigDecimal amount, Color color) {}

    @RestController
    static class ProbeController {

        @GetMapping("/probe/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Cedente", "42");
        }

        @GetMapping("/probe/optimistic-lock")
        void optimisticLock() {
            throw new OptimisticLockingFailureException("row was updated by another transaction");
        }

        @GetMapping("/probe/integrity")
        void integrity() {
            throw new DataIntegrityViolationException("duplicate key value violates constraint uk_secret");
        }

        @GetMapping("/probe/boom")
        void boom() {
            throw new IllegalStateException("internal secret detail");
        }

        @GetMapping("/probe/typed")
        String typed(@RequestParam UUID id) {
            return id.toString();
        }

        @PostMapping("/probe/body")
        ProbeRequest body(@Valid @RequestBody ProbeRequest request) {
            return request;
        }

        @PostMapping("/probe/conditional")
        String conditional(@RequestHeader(HttpHeaders.IF_MATCH) String ifMatch) {
            return ifMatch;
        }

        @PostMapping("/probe/headed")
        String headed(@RequestHeader("X-Required") String value) {
            return value;
        }
    }

    @Test
    void businessExceptionBecomesProblemDetailWithCodeAndCorrelationId() throws Exception {
        mvc.perform(get("/probe/not-found").header(CorrelationIdFilter.HEADER, "abc-123"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(CorrelationIdFilter.HEADER, "abc-123"))
                .andExpect(jsonPath("$.type").value("https://srm.com.br/problems/resource-not-found"))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Cedente com identificador 42 não existe."))
                .andExpect(jsonPath("$.instance").value("/probe/not-found"))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.correlationId").value("abc-123"));
    }

    @Test
    void beanValidationErrorsAreListedPerFieldInPortuguese() throws Exception {
        mvc.perform(post("/probe/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"amount\":\"-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[?(@.field == 'amount')].message").value("deve ser maior que 0"))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')].message").value("não deve estar em branco"));
    }

    @Test
    void malformedJsonIsReportedWithoutParserDetails() throws Exception {
        mvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.detail").value(not(containsString("Unexpected"))));
    }

    @Test
    void invalidEnumValueIsReportedOnTheField() throws Exception {
        mvc.perform(post("/probe/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"amount\":\"1\",\"color\":\"BLUE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("color"))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("[RED, GREEN]")));
    }

    @Test
    void typeMismatchOnQueryParameterIsAValidationError() throws Exception {
        mvc.perform(get("/probe/typed").param("id", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("id"));
    }

    @Test
    void missingRequiredParameterIsAValidationError() throws Exception {
        mvc.perform(get("/probe/typed"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("id"));
    }

    @Test
    void missingIfMatchMeansPreconditionRequired() throws Exception {
        mvc.perform(post("/probe/conditional"))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
    }

    @Test
    void otherMissingHeadersAreValidationErrors() throws Exception {
        mvc.perform(post("/probe/headed"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("X-Required"));
    }

    @Test
    void optimisticLockFailureIsAConcurrentModificationConflict() throws Exception {
        mvc.perform(get("/probe/optimistic-lock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
    }

    @Test
    void integrityViolationIsAConflictThatDoesNotLeakConstraintNames() throws Exception {
        mvc.perform(get("/probe/integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(content().string(not(containsString("uk_secret"))));
    }

    @Test
    void unexpectedErrorsDoNotLeakInternals() throws Exception {
        mvc.perform(get("/probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("secret"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void unknownRouteIsNotFound() throws Exception {
        mvc.perform(get("/probe/nowhere"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void unsupportedMethodIsMethodNotAllowed() throws Exception {
        mvc.perform(delete("/probe/not-found"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists(HttpHeaders.ALLOW))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }
}
