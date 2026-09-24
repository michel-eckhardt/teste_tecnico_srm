# Architecture Decision Records

Registro das decisões difíceis ou com trade-offs relevantes (formato de Michael Nygard: contexto, decisão, alternativas, consequências).

| ADR | Decisão | Status |
|---|---|---|
| [0001](0001-java-21-spring-boot-4.md) | Java 21 + Spring Boot 4 no backend | Aceita |
| [0002](0002-postgresql-acid-numeric.md) | PostgreSQL relacional com `NUMERIC` e transações ACID (SQL vs NoSQL) | Aceita |
| [0003](0003-modular-monolith-three-layers.md) | Monólito modular em três camadas (vs microsserviços) | Aceita |
| [0004](0004-pricing-strategy-and-decimal-policy.md) | Strategy por tipo de recebível e política numérica (BigDecimal, HALF_EVEN) | Aceita |
| [0005](0005-optimistic-locking-etag-if-match.md) | Concorrência otimista com `@Version`, ETag/If-Match e retry | Aceita |
| [0006](0006-idempotent-credit-assignment-creation.md) | Idempotência na criação de operações | Aceita |
| [0007](0007-native-sql-for-reports.md) | SQL nativo (JdbcClient) para o extrato em vez de ORM | Aceita |
| [0008](0008-fx-provider-resilience.md) | Câmbio: Frankfurter fora do caminho crítico, retry + circuit breaker | Aceita |
| [0009](0009-frontend-stack-and-state.md) | Frontend: React, TanStack Query + Zustand, tipos gerados do OpenAPI | Aceita |
| [0010](0010-git-workflow.md) | GitHub Flow com histórico linear, Conventional Commits e Husky | Aceita |
