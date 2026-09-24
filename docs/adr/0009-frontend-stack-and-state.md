# ADR-0009 — Frontend: React, TanStack Query + Zustand, tipos gerados do OpenAPI

- **Status:** Aceita
- **Data:** 2026-09-23

## Contexto

A SPA precisa de simulação em tempo real, grid com paginação/filtros no servidor e separação clara entre apresentação e lógica/estado. Valores monetários não podem perder precisão no navegador.

## Decisão

- **React 19 + TypeScript estrito + Vite**, UI com **Mantine** e **TanStack Table** (modo manual/server-side).
- **Estado dividido por natureza:** estado do servidor em **TanStack Query** (cache, `keepPreviousData`, invalidação dirigida); estado global do cliente — o lote em montagem — em **Zustand** (persistido em `sessionStorage`); filtros, paginação e operação aberta **na URL**; formulários com **React Hook Form + Zod**.
- **Arquitetura por feature** (`app / pages / features / shared`): em cada feature, `components/` só apresentação (props in, callbacks out), `hooks/` e `api/` com dados e regras, `model/` com schemas e regras puras. Fronteiras verificadas pelo ESLint.
- **Contrato tipado:** tipos gerados do OpenAPI do backend (`openapi-typescript`); o documento declara campos obrigatórios/anuláveis e erros `Problem`, então o frontend usa os tipos gerados diretamente.
- **Precisão:** dinheiro trafega e é formatado como string (`Intl.NumberFormat` aceita strings decimais); o cliente nunca faz aritmética financeira — o cálculo é sempre do servidor.
- Erros RFC 9457 tratados por `code` (não por mensagem), exibindo o `correlationId` para suporte.

## Alternativas consideradas

- **Angular** — muito comum no mercado financeiro; React foi escolhido pelo ecossistema de dados (TanStack) e pela leveza do setup.
- **Redux Toolkit** — robusto, mas a maior parte do estado é do servidor; Zustand cobre o pouco estado global com menos cerimônia.
- **Recalcular o preço no cliente** — duplicaria a regra e arriscaria divergência de arredondamento.

## Consequências

- 118 testes (Vitest + Testing Library + MSW) com ~90% de cobertura; bundle inicial ~280 KB gzip com páginas carregadas sob demanda.
- Mudanças de contrato no backend quebram o build do frontend em um único lugar (`shared/api/contract.ts`).
