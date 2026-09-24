# ADR-0005 — Concorrência otimista com `@Version`, ETag/If-Match e retry

- **Status:** Aceita
- **Data:** 2026-09-23

## Contexto

Dois operadores (ou duas abas) podem liquidar a mesma operação ao mesmo tempo; liquidações de operações diferentes na mesma moeda disputam a mesma conta-caixa. Nenhum dos casos pode gerar débito duplo ou saldo inconsistente.

## Decisão

- `@Version` em `CreditAssignment` e `FundCashAccount` (optimistic locking do JPA).
- A API expõe a versão como **`ETag`** e exige **`If-Match`** nos comandos de liquidação e cancelamento: sem header → `428`; versão divergente → `412`; operação já liquidada/cancelada → `409`.
- A liquidação é **uma transação**: transição de estado no domínio → `flush` da operação (o conflito na mesma operação aparece aqui, antes de tocar no caixa) → débito da conta-caixa → movimento no ledger.
- Colisões na conta-caixa entre operações **diferentes** são transitórias: a fachada `SettlementService` usa o `@Retryable` do Spring Framework 7 (backoff exponencial com jitter) **em volta** da transação inteira. No retry, uma operação que já foi liquidada por outro pedido resulta em `412`/`409`, nunca em débito duplo.
- Rede de segurança no banco: índice único parcial "um `DEBIT` por operação" e `CHECK (balance >= 0)`.

## Alternativas consideradas

- **Lock pessimista (`SELECT … FOR UPDATE`)** — correto, mas serializa leituras e segura locks durante a transação; pior sob contenção e sujeito a deadlocks entre operação e conta.
- **Sem If-Match (last-write-wins)** — o operador poderia liquidar com base em dados que outra pessoa alterou.

## Consequências

- Testado com concorrência real (Testcontainers): N threads liquidando a mesma operação → exatamente 1 sucesso e 1 débito; N operações distintas na mesma moeda → todas liquidam e o saldo final é exato.
- Conflitos são medidos (`srm_credit_assignments_settlement_conflicts_total{reason}`) e visíveis no Grafana.
- A SPA trata `412` recarregando a operação e explicando o motivo ao usuário.
