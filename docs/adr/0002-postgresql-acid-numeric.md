# ADR-0002 — PostgreSQL com `NUMERIC` e transações ACID

- **Status:** Aceita
- **Data:** 2026-09-23

## Contexto

Liquidação é dinheiro saindo do fundo: status da operação, saldo da conta-caixa e ledger precisam mudar juntos ("nenhuma liquidação pela metade"). O extrato precisa filtrar grandes volumes por período, cedente e moeda. Os dados são fortemente relacionais (cedente → operação → recebíveis → taxas).

## Decisão

PostgreSQL 17 como banco único e fonte de verdade financeira:

- `NUMERIC(19,2)` para valores e `NUMERIC(19,8)` para taxas; `TIMESTAMPTZ` em UTC.
- Integridade no próprio banco: FKs, `UNIQUE`, CHECKs de invariantes (`face = deságio + líquido`, ciclo de vida do status, `saldo >= 0`) e índice único parcial (um débito por operação).
- Schema versionado com Flyway; Hibernate apenas valida o mapeamento (`ddl-auto=validate`).

## Alternativas consideradas

- **NoSQL documental (MongoDB)** — modelaria o lote como documento, mas transações multi-documento e consultas analíticas com joins/filtros combinados são menos naturais; perderíamos CHECK/FK como rede de segurança.
- **Event store / ledger dedicado** — adequado em escala muito maior; complexidade injustificada aqui (ver "Próximos passos" no README).

## Consequências

- ACID resolve atomicidade e isolamento; a concorrência entre transações é tratada com versionamento otimista ([ADR-0005](0005-optimistic-locking-etag-if-match.md)).
- A conta-caixa por moeda é uma linha "quente" sob alta concorrência — aceitável no volume atual, com mitigação descrita no README (sub-contas/ledger particionado).
