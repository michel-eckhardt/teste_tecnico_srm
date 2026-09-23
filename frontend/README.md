# SRM Credit Engine — frontend

SPA do operador da mesa: simulação de deságio em tempo real, montagem e liquidação de lotes de
recebíveis, extrato de transações e gestão de câmbio.

**Stack:** React 19 · TypeScript (strict) · Vite 8 · React Router 7 · TanStack Query 5 · Mantine 9 ·
openapi-fetch (tipos gerados do OpenAPI do backend) · Vitest + Testing Library + MSW.

## Pré-requisitos

- Node.js 22.12+ e npm.
- Backend rodando em `http://localhost:8080` (veja [`../backend/README.md`](../backend/README.md)).

## Scripts

| Script                                                 | Descrição                                                                               |
| ------------------------------------------------------ | --------------------------------------------------------------------------------------- |
| `npm run dev`                                          | servidor de desenvolvimento em `http://localhost:5173` (proxy de `/api` para o backend) |
| `npm run build`                                        | checagem de tipos e build de produção em `dist/`                                        |
| `npm run preview`                                      | serve o build de produção (também com proxy de `/api`)                                  |
| `npm run lint` · `npm run typecheck`                   | ESLint (type-aware, a11y, fronteiras de importação) · `tsc -b`                          |
| `npm run format` · `npm run format:check`              | Prettier                                                                                |
| `npm test` · `npm run test:watch` · `npm run coverage` | Vitest (jsdom + MSW)                                                                    |
| `npm run gen:api`                                      | regenera `src/shared/api/schema.d.ts` a partir de `http://localhost:8080/v3/api-docs`   |

O destino do proxy pode ser alterado com `API_PROXY_TARGET` (veja `.env.example`).

> Documentação completa (arquitetura, decisões e Docker) será concluída ao final do desenvolvimento.
