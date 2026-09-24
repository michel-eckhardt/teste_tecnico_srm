# ADR-0001 — Java 21 + Spring Boot 4 no backend

- **Status:** Aceita
- **Data:** 2026-09-23

## Contexto

O motor precifica e liquida ativos financeiros: precisa de tipagem forte, aritmética decimal exata, transações confiáveis, observabilidade madura e um ecossistema de bibliotecas estável. O desafio valoriza "tipagem forte e frameworks maduros".

## Decisão

Java 21 (LTS) com Spring Boot 4.1 (Spring Framework 7, Hibernate 7, Jackson 3).

- `BigDecimal` nativo e `MathContext.DECIMAL128` para precisão; `records` para DTOs e value objects imutáveis.
- Spring Boot 4: starters modulares, ProblemDetail (RFC 9457) nativo, `RestClient`, `JdbcClient`, logs estruturados (ECS) nativos, `@Retryable` do Spring Framework 7 e integração com Micrometer/Actuator.
- Bibliotecas: springdoc-openapi 3 (OpenAPI 3.1), Resilience4j 2.4 (módulo para Boot 4), Flyway, Testcontainers 2, ArchUnit, big-math.

## Alternativas consideradas

- **Kotlin + Spring** — mais conciso, mas a equipe-alvo (vaga Java) e o ecossistema de exemplos favorecem Java; ganho marginal.
- **Spring Boot 3.x** — mais material disponível, porém Boot 4 é a linha atual e traz resiliência nativa, logs estruturados e JSpecify; o risco de compatibilidade foi mitigado verificando as versões (springdoc 3.1 ↔ Boot 4.1, `resilience4j-spring-boot4`).
- **Node/TypeScript ou Go** — viáveis, mas com menos suporte "de fábrica" a decimal exato e transações declarativas.

## Consequências

- Mudanças de pacote/artefato do Boot 4 e Jackson 3 exigiram atenção (ex.: `spring-boot-starter-flyway` obrigatório, `tools.jackson.*`); ver [AI_USAGE.md](../../AI_USAGE.md).
- Virtual threads ficam disponíveis como otimização futura para I/O (não habilitadas para manter o comportamento do pool JDBC previsível).
