# ADR-0008 — Câmbio: Frankfurter fora do caminho crítico, retry + circuit breaker

- **Status:** Aceita
- **Data:** 2026-09-23

## Contexto

As operações cross-currency (título em BRL, pagamento em USD e vice-versa) dependem de uma taxa de câmbio. A fonte escolhida é a API pública **Frankfurter** (dados do BCE, sem chave, atualizados em dias úteis). Uma dependência externa não pode derrubar nem deixar lenta a precificação.

## Decisão

- **A precificação nunca chama a Frankfurter.** Ela lê a última taxa persistida (par direto ou inverso derivado). A tabela é alimentada por: sincronização no startup, job agendado (cron configurável) e `POST /exchange-rates/sync`; também é possível cadastrar taxa manual.
- Cliente `RestClient` com timeouts explícitos (connect 2 s, read 3 s) protegido por **Resilience4j**: **Retry** (3 tentativas, backoff exponencial com jitter, só para I/O/timeout/5xx — 4xx não é repetido) envolvendo um **Circuit Breaker** (janela de 10 chamadas, 50% de falhas, 60 s aberto). Falha/circuito aberto → `503 FX_PROVIDER_UNAVAILABLE` no endpoint de sync; o job apenas registra e mantém a última taxa.
- **Controle de risco:** cross-currency com taxa mais antiga que `srm.fx.max-rate-age` (5 dias, cobre fins de semana e feriados do BCE) é recusado com `422 EXCHANGE_RATE_STALE`.
- **Taxa `SEED`** na migração para operar offline; taxas observadas (`FRANKFURTER`/`MANUAL`) **sempre** têm precedência sobre ela.
- O circuit breaker aparece no health (sem derrubar a readiness) e no Grafana.

## Alternativas consideradas

- **Chamar o provedor a cada simulação** — latência e disponibilidade da precificação atreladas a um serviço externo; inaceitável para simulação em tempo real.
- **Provedor pago com SLA** — recomendado em produção (ex.: PTAX do BCB para BRL); a porta `ExchangeRateProvider` permite trocar sem tocar no domínio.

## Consequências

- A precificação continua funcionando com a Frankfurter fora do ar (até o limite de staleness).
- Testes com WireMock cobrem sucesso, 5xx com retries, timeout, 4xx sem retry, JSON inválido e abertura do circuito; nenhum teste depende da rede.
- Lição registrada: a `SEED` datada no dia do deploy "vencia" a cotação real (o BCE publica o dia útil anterior até a tarde) — corrigido pela precedência das taxas observadas.
