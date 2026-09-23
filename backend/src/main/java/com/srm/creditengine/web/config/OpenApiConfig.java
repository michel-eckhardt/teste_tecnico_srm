package com.srm.creditengine.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import java.math.BigDecimal;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 document served at {@code /v3/api-docs} (Swagger UI at {@code /swagger-ui.html}).
 * Decimals are documented as strings, matching the runtime JSON representation.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    static final String API_VERSION = "v1";

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
                        .contact(new Contact().name("SRM Asset - Engenharia")));
    }
}
