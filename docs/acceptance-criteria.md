# Critérios de aceite

Critérios no formato **Dado / Quando / Então**, agrupados por qualidade. Cada um aponta como é verificado: teste automatizado (classe/arquivo), medição ou inspeção. Os números medidos estão no [README](../README.md#qualidade-e-verificação).

## 1. Funcionais

### Precificação

- **CA-01 — Fórmula e Strategy.** *Dado* uma duplicata mercantil de R$ 10.000,00 com vencimento em 90 dias (taxa base BRL 1% a.m.), *quando* simulada, *então* o valor presente é **R$ 9.285,99** e o deságio R$ 714,01 (spread 1,5% a.m.); para cheque pré-datado (2,5% a.m.), **R$ 9.019,43**.
  *Verificação:* `PricingEngineTest` (parametrizado, valores calculados à parte com 50 dígitos), `PricingApiIT`.
- **CA-02 — Prazo fracionário.** *Dado* um prazo que não é múltiplo de 30 dias, *então* o expoente é `dias/30` fracionário, sem uso de `double`. *Verificação:* `PricingEngineTest`.
- **CA-03 — Cross-currency no final.** *Dado* título em BRL pago em USD, *quando* precificado, *então* a conversão é aplicada sobre o valor presente não arredondado e há um único arredondamento HALF_EVEN na moeda de pagamento. *Verificação:* `PricingEngineTest`, `PricingApiIT`.
- **CA-04 — Validação.** *Dado* valor ≤ 0, mais de 2 casas, vencimento não futuro, prazo acima de 5 anos ou tipo desconhecido, *então* a API responde 400/422 com `code` e campo inválido, sem precificar. *Verificação:* `PricingControllerTest`, `PricingApiIT`, `receivable-form.test.ts`.

### Câmbio

- **CA-05 — Taxa vigente e inversa.** *Dado* USD/BRL persistido, *quando* consultado BRL/USD, *então* a taxa é derivada (`derived=true`). *Verificação:* `ExchangeRateServiceTest`, `ExchangeRateApiIT`.
- **CA-06 — Precedência das observadas.** *Dado* uma taxa `SEED` mais recente que a última cotação da Frankfurter, *então* a vigente é a da Frankfurter. *Verificação:* `ExchangeRateServiceTest`.
- **CA-07 — Taxa desatualizada.** *Dada* uma taxa com mais de 5 dias, *quando* uma operação cross-currency é precificada, *então* a resposta é `422 EXCHANGE_RATE_STALE`. *Verificação:* `ExchangeRateServiceTest`, `PricingEngineTest`.

### Cessão e liquidação

- **CA-08 — Atomicidade.** *Dado* saldo insuficiente, *quando* a liquidação é pedida, *então* a resposta é `422 INSUFFICIENT_FUNDS` e nada muda (status, saldo e ledger intactos). *Verificação:* `SettlementApiIT`.
- **CA-09 — Liquidação concorrente da mesma operação.** *Dadas* N requisições simultâneas de liquidação da mesma operação, *então* exatamente uma vence, há exatamente um débito e as demais recebem 409/412. *Verificação:* `SettlementConcurrencyIT`.
- **CA-10 — Disputa pelo saldo.** *Dadas* N operações distintas liquidadas ao mesmo tempo na mesma moeda, *então* todas liquidam e o saldo final é exatamente o inicial menos a soma. *Verificação:* `SettlementConcurrencyIT`.
- **CA-11 — Pré-condições HTTP.** Liquidar/cancelar sem `If-Match` → `428`; com versão antiga → `412`; operação já liquidada → `409`. *Verificação:* `SettlementApiIT`; na SPA, `CreditAssignmentView.test.tsx` (412 recarrega a operação e explica).
- **CA-12 — Idempotência.** Mesma `Idempotency-Key` e mesmo payload → a mesma operação (`200`); payload diferente → `409 IDEMPOTENCY_KEY_REUSED`; 8 envios concorrentes → uma linha. *Verificação:* `CreditAssignmentApiIT`, `SettlementConcurrencyIT`, `CreditAssignmentServiceTest`.

### Extrato

- **CA-13 — Filtros dinâmicos.** Filtros por período (inclusivo, no fuso de São Paulo, inclusive nas viradas de meia-noite), cedente, moeda e status, isolados ou combinados, com paginação e total corretos. *Verificação:* `SettlementStatementIT`, `TransactionsExplorer.test.tsx`, `statement-filters.test.ts`.
- **CA-14 — Ordenação segura.** Somente `createdAt`, `settledAt`, `totalNetAmount` e `assignorName`; qualquer outro valor (inclusive tentativas de injeção) → `400`. *Verificação:* `SettlementStatementSortTest`, `SettlementStatementIT`.

## 2. Usabilidade

- **CA-15 — Simulação em tempo real.** *Quando* o operador altera valor, vencimento, tipo ou moedas, *então* o valor líquido é recalculado automaticamente (debounce de 300 ms), sem piscar o resultado anterior, com o detalhamento (prazo, taxas, valor presente, deságio, câmbio). *Verificação:* `PricingSimulator.test.tsx`; inspeção no navegador.
- **CA-16 — Estado compartilhável.** Filtros, página e ordenação do grid ficam na URL (links compartilháveis, botão voltar funciona). *Verificação:* `useUrlState.test.tsx`, `TransactionsExplorer.test.tsx`.
- **CA-17 — Erros compreensíveis.** Toda falha mostra título e detalhe em português e o código de suporte (`correlationId`); conflitos de liquidação explicam o que aconteceu. *Verificação:* `ErrorAlert.test.tsx`, `command-errors.test.ts`, `problem.test.ts`.
- **CA-18 — Formatos brasileiros.** Moeda, datas (`DD/MM/AAAA`) e CNPJ formatados em pt-BR a partir de strings, sem perda de precisão. *Verificação:* `money.test.ts`, `decimal.test.ts`, `date.test.ts`, `cnpj.test.ts`.

## 3. Segurança

- **CA-19 — Validação de entrada** em todas as bordas (Bean Validation + Zod no cliente), incluindo CNPJ com dígitos verificadores e limites de tamanho do lote (1..500). *Verificação:* `GlobalExceptionHandlerTest`, `AssignorApiIT`, `assignor-form.test.ts`.
- **CA-20 — Sem vazamento de detalhes internos.** Erros inesperados retornam `500 INTERNAL_ERROR` sem stack trace; logs não contêm dados sensíveis. *Verificação:* `GlobalExceptionHandlerTest`.
- **CA-21 — SQL parametrizado** em todas as consultas; ordenação por whitelist. *Verificação:* CA-14 + revisão de código.
- **CA-22 — Superfície mínima.** PostgreSQL não exposto fora da rede do compose; containers sem root; nginx com CSP, `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy`; CORS restrito às origens configuradas. *Verificação:* inspeção (`docker-compose.yml`, Dockerfiles, `nginx.conf`, `WebConfigTest`).
- **Fora do escopo (documentado):** autenticação/autorização — próximo passo com OAuth2/OIDC (resource server) e perfis de operador/aprovador.

## 4. Desempenho

- **CA-23 — Extrato em volume.** *Dado* um banco com 100 mil operações, *quando* o extrato é consultado com filtros, ordenações e páginas profundas, *então* o **p95 < 300 ms**. *Medido:* p95 = **72 ms** (p50 = 11,7 ms) em 108 requisições cobrindo 9 combinações; `EXPLAIN ANALYZE` entre 0,14 ms e 28 ms.
- **CA-24 — Precificação.** A simulação não faz chamadas externas (câmbio vem do banco); p95 do motor visível no Grafana (`srm_pricing_duration_seconds`). *Medido:* ~4 ms de p95 do motor no ambiente local.
- **CA-25 — Frontend.** JS inicial ≤ 300 KB gzip, páginas carregadas sob demanda. *Medido:* ~280 KB (páginas com ~4 KB cada).

## 5. Escalabilidade e resiliência

- **CA-26 — API stateless.** Nenhum estado de sessão no servidor; réplicas podem ser adicionadas atrás de um balanceador (sessão de lote fica no cliente, idempotência e locks no banco).
- **CA-27 — Dependência externa isolada.** *Dada* a Frankfurter indisponível, *então* o sync responde `503 FX_PROVIDER_UNAVAILABLE`, o circuit breaker abre após o limiar, a readiness continua `UP` e a precificação segue com a última taxa válida. *Verificação:* `FrankfurterExchangeRateProviderIT`, `ExchangeRateSyncIT`, `ActuatorIT`.
- **CA-28 — Observabilidade.** Logs JSON (ECS) com `traceId`, `spanId` e `correlationId`; métricas técnicas e de negócio no Prometheus com séries iniciadas em zero; dashboard provisionado. *Verificação:* `LogCorrelationIT`, `BusinessMetricsIT`, `BusinessMetricsTest`, `ActuatorIT`; inspeção do Grafana.
- **CA-29 — Arquitetura verificável.** Camadas e ausência de ciclos entre módulos garantidas por teste. *Verificação:* `LayeredArchitectureTest` (ArchUnit).
- **CA-30 — Contrato estável.** O OpenAPI declara campos obrigatórios/anuláveis e respostas de erro; o frontend gera tipos a partir dele. *Verificação:* `OpenApiDocumentIT`, `npm run typecheck`.
