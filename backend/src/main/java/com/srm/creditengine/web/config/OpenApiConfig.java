package com.srm.creditengine.web.config;

import com.srm.creditengine.web.support.ProblemResponses;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenAPI 3.1 document served at {@code /v3/api-docs} (Swagger UI at {@code /swagger-ui.html}).
 *
 * <ul>
 *   <li>decimals are documented as strings, matching the runtime JSON representation;
 *   <li>response models declare required and nullable fields ({@link ResponseSchemaConverter});
 *   <li>error responses are documented as RFC 9457 problems ({@code Problem} schema): 400 for
 *       operations with input, 404 for operations addressing a resource by id, 500 for all, plus the
 *       statuses listed in {@link ProblemResponses}.
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    static final String API_VERSION = "v1";
    static final String PROBLEM_SCHEMA = "Problem";
    static final String PROBLEM_MEDIA_TYPE = "application/problem+json";

    private static final Map<Integer, String> PROBLEM_DESCRIPTIONS = Map.of(
            400, "Requisição inválida (validação, formato ou parâmetro)",
            404, "Recurso não encontrado",
            409, "Conflito de estado (operação já liquidada/cancelada, concorrência, duplicidade)",
            412, "If-Match não corresponde à versão atual do recurso",
            422, "Regra de negócio violada",
            428, "If-Match ausente",
            500, "Erro inesperado (sem detalhes internos)",
            503, "Provedor de câmbio indisponível (circuit breaker aberto ou falha após retries)");

    static {
        SpringDocUtils.getConfig()
                .replaceWithSchema(
                        BigDecimal.class,
                        new StringSchema()
                                .format("decimal")
                                .pattern("^-?\\d+(\\.\\d+)?$")
                                .example("10000.00"));
    }

    @Bean
    OpenAPI srmCreditEngineOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SRM Credit Engine API")
                        .version(API_VERSION)
                        .description("""
                                Plataforma de cessão de crédito multimoedas (BRL/USD) de um FIDC: câmbio, \
                                precificação de recebíveis (Strategy por tipo), cessão e liquidação com \
                                controle otimista de concorrência e extrato analítico.

                                Convenções: valores monetários e taxas trafegam como string decimal; erros \
                                seguem a RFC 9457 (application/problem+json) com `code` e `correlationId`; \
                                comandos que alteram estado exigem `If-Match` com o ETag atual.""")
                        .contact(new Contact().name("SRM Asset - Engenharia")))
                .components(new Components()
                        .addSchemas("FieldViolation", fieldViolationSchema())
                        .addSchemas(PROBLEM_SCHEMA, problemSchema()));
    }

    @Bean
    OperationCustomizer problemResponsesCustomizer() {
        return (operation, handlerMethod) -> {
            Set<Integer> statuses = new TreeSet<>(List.of(500));
            if (handlerMethod.getMethodParameters().length > 0) {
                statuses.add(400);
            }
            if (Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(parameter -> parameter.hasParameterAnnotation(PathVariable.class))) {
                statuses.add(404);
            }
            ProblemResponses declared = handlerMethod.getMethodAnnotation(ProblemResponses.class);
            if (declared != null) {
                Arrays.stream(declared.value()).forEach(statuses::add);
            }
            ApiResponses responses = operation.getResponses();
            statuses.forEach(status -> responses.putIfAbsent(String.valueOf(status), problemResponse(status)));
            return operation;
        };
    }

    private static ApiResponse problemResponse(int status) {
        String description = PROBLEM_DESCRIPTIONS.getOrDefault(status, "Erro");
        return new ApiResponse()
                .description(description)
                .content(new Content()
                        .addMediaType(
                                PROBLEM_MEDIA_TYPE,
                                new MediaType()
                                        .schema(new JsonSchema().$ref("#/components/schemas/" + PROBLEM_SCHEMA))));
    }

    @SuppressWarnings("rawtypes")
    private static Schema<?> problemSchema() {
        Map<String, Schema> properties = new LinkedHashMap<>();
        properties.put("type", string("URI que identifica o tipo do problema").format("uri"));
        properties.put("title", string("Resumo do problema, em português"));
        properties.put("status", new JsonSchema().types(Set.of("integer")).description("Status HTTP"));
        properties.put("detail", string("Explicação desta ocorrência, em português"));
        properties.put("instance", string("Caminho da requisição").format("uri-reference"));
        properties.put("code", string("Código estável do erro para tratamento no cliente (ex.: INSUFFICIENT_FUNDS)"));
        properties.put("correlationId", string("Mesmo valor do header X-Correlation-Id e dos logs"));
        properties.put(
                "errors",
                new JsonSchema()
                        .types(Set.of("array"))
                        .items(new JsonSchema().$ref("#/components/schemas/FieldViolation"))
                        .description("Violações por campo (somente em erros de validação)"));
        JsonSchema problem = new JsonSchema();
        problem.types(Set.of("object"))
                .description("Problem Details (RFC 9457) com as extensões code, correlationId e errors")
                .required(List.of("type", "title", "status", "code", "correlationId"));
        problem.setProperties(properties);
        return problem;
    }

    @SuppressWarnings("rawtypes")
    private static Schema<?> fieldViolationSchema() {
        Map<String, Schema> properties = new LinkedHashMap<>();
        properties.put("field", string("Campo inválido (ex.: receivables[0].faceValue)"));
        properties.put("message", string("Mensagem de validação, em português"));
        JsonSchema violation = new JsonSchema();
        violation.types(Set.of("object")).required(List.of("field", "message"));
        violation.setProperties(properties);
        return violation;
    }

    private static Schema<?> string(String description) {
        return new JsonSchema().types(Set.of("string")).description(description);
    }
}
