# ADR-0006 — Idempotência na criação de operações

- **Status:** Aceita
- **Data:** 2026-09-23

## Contexto

`POST /credit-assignments` não é idempotente por natureza. Um timeout de rede depois de o servidor ter gravado a operação leva o cliente a reenviar e criar uma cessão duplicada — um erro financeiro real.

## Decisão

- Header `Idempotency-Key` (formato `[A-Za-z0-9._:-]{1,100}`). A SPA gera um UUID para o lote e o mantém enquanto o lote não muda, então reenviar depois de um erro de rede reutiliza a mesma chave.
- A operação guarda a chave (`UNIQUE`) e o SHA-256 do payload canônico (valores numericamente iguais, como `"10000"` e `"10000.00"`, geram o mesmo hash).
- Mesma chave + mesmo payload → devolve a operação existente (`200` + `Content-Location`). Mesma chave + payload diferente → `409 IDEMPOTENCY_KEY_REUSED`.
- Corrida entre requisições idênticas simultâneas: a violação da constraint única é capturada e respondida com a operação vencedora.

## Alternativas consideradas

- **Tabela separada de chaves com TTL** — generaliza para outros endpoints, mas adiciona uma escrita extra; desnecessário com um único endpoint de criação.
- **Deduplicação por conteúdo** (sem chave) — bloquearia lotes legítimos idênticos.

## Consequências

- Retries do cliente são seguros; métricas de "operações criadas" excluem os replays.
- Testado com 8 criações concorrentes com a mesma chave → uma única linha.
