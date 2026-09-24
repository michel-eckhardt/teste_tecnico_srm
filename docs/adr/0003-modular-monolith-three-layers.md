# ADR-0003 — Monólito modular em três camadas

- **Status:** Aceita
- **Data:** 2026-09-23

## Contexto

O desafio pede separação entre aplicação, negócio e persistência (3 camadas), permitindo que relatórios usem apenas duas. O domínio é coeso (câmbio → precificação → cessão → liquidação) e a liquidação precisa de transação local entre operação, caixa e ledger.

## Decisão

Um único deployable (API) organizado em camadas e módulos:

- `web` (aplicação): controllers, DTOs, validação, ProblemDetail, correlation-id, OpenAPI.
- `domain` (negócio): módulos `currency`, `pricing`, `assignment`, `assignor`, `treasury`, `common`.
- `persistence`: repositórios Spring Data; `persistence.report` com SQL nativo.
- `integration.fx`: adaptador da Frankfurter (implementa uma porta do domínio de câmbio).
- Exceção prevista: o extrato é `web.report → persistence.report` (duas camadas).

As regras (quem pode depender de quem, sem ciclos entre módulos de domínio) são **testes ArchUnit**, não só convenção.

## Alternativas consideradas

- **Microsserviços (pricing, câmbio, liquidação)** — isolamento de deploy e escala, mas a liquidação passaria a exigir sagas/consistência eventual para algo que hoje é uma transação local; custo operacional alto para o tamanho do problema.
- **Arquitetura hexagonal completa** (entidades de domínio sem JPA) — mais pura, porém duplicaria modelos; usamos portas apenas onde há integração externa (provedor de câmbio).

## Consequências

- Deploy e testes simples; a API é stateless e escala horizontalmente.
- Os módulos já têm fronteiras verificadas — um módulo (ex.: câmbio) pode virar serviço próprio no futuro sem reescrever o domínio.
