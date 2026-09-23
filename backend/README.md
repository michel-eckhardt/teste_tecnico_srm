# SRM Credit Engine — backend

API REST de cessão de crédito multimoedas (BRL/USD): câmbio, precificação de recebíveis por Strategy,
cessão com idempotência, liquidação atômica com controle otimista de concorrência e extrato analítico.

**Stack:** Java 21 · Spring Boot 4.1 (Spring Framework 7, Hibernate 7, Jackson 3) · PostgreSQL 17 · Flyway ·
Resilience4j · Micrometer (Prometheus + tracing OpenTelemetry) · springdoc-openapi · Testcontainers · WireMock · ArchUnit.

## Pré-requisitos

- JDK 21 e Docker (Testcontainers sobe o PostgreSQL dos testes e do modo `test-run`).
- Maven não é necessário: use o wrapper `./mvnw` (`mvnw.cmd` no Windows).

## Build e testes

```bash
./mvnw verify            # compila, testes unitários (*Test), integração (*IT), Spotless, JaCoCo
./mvnw spotless:apply    # formata o código (palantir-java-format)
```

- Testes de integração usam PostgreSQL 17 real (Testcontainers) e um WireMock no lugar da API Frankfurter:
  nenhum teste acessa a rede.
- Cobertura: `target/site/jacoco/index.html`; o build falha se algum pacote `domain` ficar abaixo de 80% de linhas.
- `LayeredArchitectureTest` (ArchUnit) garante as regras de camadas.

## Executar localmente

```bash
./mvnw spring-boot:test-run          # sobe um PostgreSQL descartável via Testcontainers
```

Com um PostgreSQL próprio:

```bash
DB_URL=jdbc:postgresql://localhost:5432/srm_credit_engine DB_USER=srm DB_PASSWORD=srm ./mvnw spring-boot:run
```

Imagem Docker (multi-stage, JRE Alpine, usuário não-root, `HEALTHCHECK` na liveness):

```bash
docker build -t srm-credit-engine-backend .
docker run -p 8080:8080 -e DB_URL=jdbc:postgresql://<host>:5432/srm_credit_engine srm-credit-engine-backend
```

## Perfis

| Perfil | Uso |
|---|---|
| _default_ | desenvolvimento local; sincronização com a Frankfurter desligada |
| `docker` | ativo na imagem; logs estruturados JSON (ECS) e sincronização automática de câmbio (startup + cron) |
| `demo` | adiciona `classpath:db/demo`: ~200 cedentes e 100.000 operações nos últimos 12 meses (~15 s de migração) |
| `test` | testes automatizados: timeouts curtos, sem sincronização automática |

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/srm_credit_engine` | JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `srm` / `srm` | credenciais |
| `DB_POOL_SIZE` | `10` | tamanho do pool Hikari |
| `SERVER_PORT` | `8080` | porta HTTP |
| `SPRING_PROFILES_ACTIVE` | — (`docker` na imagem) | perfis |
| `FX_SYNC_ENABLED` | `false` (`true` em `docker`/`demo`) | sincronização automática USD/BRL |
| `FRANKFURTER_BASE_URL` | `https://api.frankfurter.dev/v1` | API de câmbio (ECB) |
| `TRACING_ENABLED` | `true` | traceId/spanId nos logs (nenhum exporter configurado) |

Parâmetros de negócio (`application.yml`, prefixo `srm.`): fuso de negócio (`business.zone-id`), idade máxima
da taxa (`fx.max-rate-age`, P5D), taxas base por moeda (`pricing.base-rates`), spreads por tipo
(`pricing.spreads.*`), prazo máximo (`pricing.max-term-days`), retry de liquidação (`settlement.retry.*`) e CORS
(`web.cors.allowed-origins`, padrão `http://localhost:5173,http://localhost:3000`).

## Endpoints

Base `/api/v1` — contrato completo em [`docs/api-contract.md`](../docs/api-contract.md).

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/currencies` | moedas suportadas |
| GET | `/exchange-rates/latest?base&quote` | taxa vigente (par publicado ou inverso derivado, flag `stale`) |
| GET | `/exchange-rates?base&quote&page&size` · `/exchange-rates/{id}` | histórico / consulta |
| POST | `/exchange-rates` · `/exchange-rates/sync` | cadastro manual / sincronização Frankfurter (retry + circuit breaker) |
| GET | `/receivable-types` | tipos de recebível com o spread da Strategy |
| POST | `/pricing/simulations` | simulação de deságio (não persiste) |
| POST / GET | `/assignors` · `/assignors/{id}` | cedentes (CNPJ validado, busca paginada) |
| POST | `/credit-assignments` | cria operação (`Idempotency-Key` opcional; `201` + `ETag`) |
| GET | `/credit-assignments/{id}` | consulta com `ETag` |
| POST | `/credit-assignments/{id}/settlement` · `/cancellation` | liquida / cancela (`If-Match` obrigatório) |
| GET | `/cash-accounts` | saldos do fundo |
| GET | `/reports/settlement-statement` | extrato (filtros, paginação e ordenação no servidor, SQL nativo) |

Documentação e operação:

- Swagger UI: <http://localhost:8080/swagger-ui.html> · OpenAPI: <http://localhost:8080/v3/api-docs>
- Actuator: `/actuator/health` (`/liveness`, `/readiness`), `/actuator/info`, `/actuator/prometheus`,
  `/actuator/metrics`, `/actuator/circuitbreakers`

## Organização do código

```
com.srm.creditengine
├── web          camada de aplicação: controllers, DTOs (records), ProblemDetail, correlation id, OpenAPI, CORS
│   └── report   extrato (acessa persistence.report direto: relatório em 2 camadas)
├── domain       camada de negócio: modelo, serviços, pricing (Strategy), máquina de estados, exceções
├── persistence  repositórios Spring Data
│   └── report   consultas analíticas com JdbcClient (SQL nativo)
├── integration  adaptador Frankfurter (porta ExchangeRateProvider) e agendamento da sincronização
└── config       beans transversais (Clock, serialização de decimais, agendamento, resiliência)
```
