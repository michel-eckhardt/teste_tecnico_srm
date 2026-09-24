# ADR-0007 — SQL nativo (JdbcClient) para o extrato

- **Status:** Aceita
- **Data:** 2026-09-23

## Contexto

O "Extrato de Liquidação" filtra grandes volumes por período, cedente, moeda e status, com paginação e ordenação no servidor. O desafio considera diferencial "Query Builders ou SQL nativo otimizado em vez de ORMs puros para relatórios", e permite que relatórios pulem a camada de negócio.

## Decisão

- `SettlementStatementRepository` com `JdbcClient` e SQL nativo: projeta apenas as colunas do extrato (nenhuma entidade é carregada) e monta o `WHERE` dinamicamente com um pequeno builder (`SqlConditions`), **sempre com parâmetros nomeados**.
- **Ordenação em whitelist:** o parâmetro `sort` é convertido num enum que produz fragmentos SQL fixos; entrada inválida → `400`. Nenhuma concatenação de entrada do usuário (sem SQL injection).
- **Período** convertido em intervalo meio-aberto de instantes (`>= início AND < fim`, no fuso de negócio), sem função sobre a coluna indexada.
- Totais denormalizados na operação (sem agregação de recebíveis); `COUNT` separado sem o `JOIN` com cedente; índices compostos `(filtro, created_at DESC)`.
- Duas camadas: `web.report` → `persistence.report`.

## Alternativas consideradas

- **Spring Data JPA + Specifications** — carregaria entidades e geraria SQL menos previsível para relatórios.
- **jOOQ** — excelente query builder tipado, mas exige geração de código no build; o ganho não compensa para uma consulta.
- **Paginação por cursor (keyset)** — mais eficiente em páginas profundas, mas o grid precisa de "ir para a página N" e total de registros.

## Consequências

- Medido com 100 mil operações: p95 de ~72 ms via HTTP (EXPLAIN: 0,1–28 ms conforme o filtro/offset). Ver o README.
- Em escala muito maior: keyset para navegação profunda, réplica de leitura e/ou índices BRIN por data (ver "Próximos passos").
