# SRM Credit Engine — frontend

SPA do operador da mesa de cessão de crédito: simulação de deságio em tempo real, montagem e
registro de lotes de recebíveis, liquidação/cancelamento com controle de concorrência, extrato de
transações com filtros e paginação no servidor, e gestão de câmbio.

**Stack:** React 19 · TypeScript 5.9 (strict) · Vite 8 (Rolldown) · React Router 7 · TanStack Query 5 ·
TanStack Table 8 · Zustand 5 · React Hook Form 7 + Zod 4 · Mantine 9 · openapi-fetch + openapi-typescript ·
Vitest 5 + Testing Library + MSW 2 · ESLint 10 (typescript-eslint type-aware) + Prettier.

## Funcionalidades

- **Painel do operador** (`/`): formulário do recebível (tipo com o spread da Strategy, valor de face,
  moeda do título, vencimento, moeda de pagamento) com **simulação em tempo real** — prazo, taxa base,
  spread, taxa de desconto, valor presente, deságio, câmbio aplicado e o **valor líquido** na moeda de
  pagamento. "Adicionar ao lote" monta o lote (uma moeda de pagamento por lote), com cedente (busca no
  servidor ou "Novo cedente" com CNPJ validado), totais e envio com `Idempotency-Key`. A operação
  registrada aparece com as ações **Liquidar** / **Cancelar** (`If-Match`).
- **Transações** (`/transacoes`): extrato de liquidação (100 mil operações no perfil `demo`) em
  TanStack Table **manual** — paginação (10/20/50/100) e ordenação no servidor (só colunas da
  whitelist), filtros dinâmicos (período, cedente, moeda, status) **sincronizados com a URL** e
  gaveta de detalhes com as mesmas ações de liquidação.
- **Câmbio** (`/cambio`): USD/BRL e BRL/USD vigentes (fonte, data de referência, derivada do par
  inverso, alerta de taxa desatualizada), sincronização com a Frankfurter (503 explicado), histórico
  paginado, cadastro manual e saldos das contas-caixa do fundo.

## Como rodar

### Desenvolvimento (contra o backend real)

```bash
# 1. backend (outro terminal) — PostgreSQL via Testcontainers + 100 mil operações de demonstração
cd ../backend && SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:test-run

# 2. frontend
npm ci
npm run dev        # http://localhost:5173
```

O SPA só usa URLs relativas (`/api/v1/...`): o Vite faz proxy de `/api` para `http://localhost:8080`
(alterável com `API_PROXY_TARGET`, ver `.env.example`). Em produção o nginx faz o mesmo, então não há
CORS nem endereço de backend embutido no bundle.

### Docker

```bash
docker build -t srm-credit-engine-frontend .
docker network create srm
# backend acessível como "backend:8080" na mesma rede (ex.: a imagem de ../backend)
docker run -d --name frontend --network srm -p 3000:8080 srm-credit-engine-frontend
# ou apontando para outro endereço: -e API_UPSTREAM=http://host.docker.internal:8080
```

Imagem multi-stage: `node:22-alpine` (`npm ci` + `npm run build`) → `nginxinc/nginx-unprivileged:1.30-alpine`
(usuário não-root, ~83 MB). O `nginx.conf` é um template (envsubst) com:

- fallback de SPA (`try_files ... /index.html`) e `index.html` sempre revalidado (`no-cache`);
- `/assets/*` (nomes com hash) com cache de 1 ano; gzip;
- `/api/` → `API_UPSTREAM` (padrão `http://backend:8080`), resolvido por requisição (`NGINX_RESOLVER`,
  padrão `127.0.0.11`, o DNS do Docker), então o container sobe mesmo antes do backend;
- cabeçalhos de segurança: CSP restritiva (`script-src 'self'`, `connect-src 'self'`,
  `frame-ancestors 'none'`), `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`,
  `Permissions-Policy`; `server_tokens off`;
- `/healthz` para o `HEALTHCHECK`.

### Scripts

| Script                                                 | Descrição                                                                                |
| ------------------------------------------------------ | ---------------------------------------------------------------------------------------- |
| `npm run dev` · `npm run preview`                      | servidor de desenvolvimento (5173) · build de produção (4173), ambos com proxy de `/api` |
| `npm run build`                                        | `tsc -b` + `vite build`                                                                  |
| `npm run lint` · `npm run typecheck`                   | ESLint (type-aware, a11y, fronteiras de importação, `--max-warnings 0`) · `tsc -b`       |
| `npm run format` · `npm run format:check`              | Prettier                                                                                 |
| `npm test` · `npm run test:watch` · `npm run coverage` | Vitest (jsdom + MSW)                                                                     |
| `npm run gen:api`                                      | regenera `src/shared/api/schema.d.ts` a partir de `http://localhost:8080/v3/api-docs`    |

## Arquitetura

```
src/
  app/        bootstrap: providers (Query, Mantine, datas pt-BR, notificações, modais), rotas com
              code splitting por página, AppShell, tema, política de retry, error boundaries
  pages/      composição fina das features por rota
  features/
    pricing-simulator/   formulário + simulação em tempo real
    credit-assignments/  lote (Zustand), registro com idempotência, liquidação/cancelamento
    assignors/           busca e cadastro de cedentes
    transactions/        extrato: filtros/paginação/ordenação na URL, grid, gaveta de detalhes
    exchange-rates/      taxas vigentes, sincronização, histórico, cadastro manual
    treasury/            saldos das contas-caixa
    reference-data/      moedas e tipos de recebível
  shared/
    api/      cliente tipado (openapi-fetch), ApiError + RFC 9457, tipos gerados e refinados,
              fábrica de query keys
    lib/      funções puras: dinheiro, decimais, datas, CNPJ, idempotência, rótulos, erros de formulário
    hooks/    useDebouncedValue, useUrlState
    ui/       componentes de apresentação genéricos (MoneyText, MoneyInput, StatusBadge, ErrorAlert…)
  test/       setup do Vitest, servidor MSW, handlers por recurso, fixtures no formato do contrato
```

Cada feature segue `api/` (chamadas HTTP) → `hooks/` (TanStack Query e regras) → `containers/`
(ligam hooks a componentes) → `components/` (apresentação: props de entrada, callbacks de saída) e
`model/` (schemas Zod e regras puras, testados isoladamente). Uma feature só é usada por outra
através do seu `index.ts`.

**Fronteiras verificadas pelo ESLint** (`no-restricted-imports`): `shared` não importa features,
páginas nem `app`; nada importa internals de outra feature; componentes de apresentação
(`features/*/components`, `shared/ui`) não podem importar o cliente HTTP, TanStack Query, hooks ou
containers.

### Estado

- **Estado do servidor** — TanStack Query, com chaves hierárquicas em `shared/api/query-keys.ts`:
  simulação, extrato, operação + ETag, taxas, saldos. As mutations invalidam o que mudou (liquidar →
  extrato e saldos; nova taxa → taxas e simulações em cache).
- **Estado global do cliente** — Zustand (`credit-assignments/model/batch-store.ts`), persistido em
  `sessionStorage`: o lote em montagem, sua moeda de pagamento e a chave de idempotência pendente.
- **Estado na URL** — `useUrlState` + codec Zod: filtros, página, tamanho e ordenação do extrato;
  operação aberta (`?operacao=`).
- **Estado local** — React Hook Form + Zod nos formulários.

### Decisões

- **Dinheiro nunca vira `number`.** Valores e taxas trafegam como string (contrato); a formatação usa
  `Intl.NumberFormat` com strings numéricas (exata, ES2023), somas/comparações usam centavos em
  `BigInt`, e o campo de valor usa máscara "de banco" (dígitos preenchem a partir dos centavos).
- **Tipos gerados do OpenAPI + refinamento.** O springdoc documenta toda propriedade de resposta
  como opcional e nunca como `nullable`; `shared/api/contract.ts` restaura o contrato real e prova em
  tempo de compilação (`Narrows`) que o tipo refinado só estreita o gerado — regenerar o schema após
  uma mudança no backend quebra o build em vez de quebrar em runtime.
- **Erros RFC 9457.** Toda falha vira `ApiError` (status, `code`, título/detalhe em português,
  violações por campo, `correlationId`). A UI decide pelo `code`, nunca pela mensagem. Cada requisição
  envia seu próprio `X-Correlation-Id` (reutilizado pelo backend), exibido com botão de cópia.
- **Idempotência.** O `Idempotency-Key` do lote é mantido enquanto o lote não muda: repetir após
  falha de rede é seguro (o servidor devolve a operação já criada, 200). Qualquer mudança no lote
  descarta a chave.
- **Concorrência otimista.** Liquidar/cancelar enviam `If-Match` com o ETag da versão exibida; 412,
  409 (já liquidada, transição inválida, conflito concorrente) e falha de rede recarregam a operação
  e explicam o ocorrido; 422 `INSUFFICIENT_FUNDS` mantém a tela.
- **Simulação em tempo real.** Só valores válidos (mesmas regras do backend) são enviados;
  debounce de 300 ms comparando por valor, cache por payload, `keepPreviousData` (sem "piscar") e
  `skipToken` enquanto o formulário está incompleto.
- **Retry.** Consultas repetem apenas falhas transitórias (sem resposta ou 5xx); comandos nunca são
  repetidos automaticamente.

### Por que estas versões

- **TypeScript 5.9**, não 6/7: `typescript-eslint` suporta `< 6.1` e o `openapi-typescript` declara
  peer `^5`.
- **ESLint 10** com `eslint-plugin-jsx-a11y-x` (fork do es-tooling): o `jsx-a11y` original ainda não
  suporta ESLint 10 e o ESLint 9 está fora de suporte.
- **React Router 7** e **jsdom 29**: as versões seguintes exigem Node ≥ 22.22 (o projeto roda em
  Node 22.12+).
- **TanStack Table 8**: API estável e documentada para o modo manual.

## Testes

118 testes (Vitest + Testing Library + MSW; cobertura ≈ 90% de linhas / 83% de branches):

- `shared/lib` e `shared/api`: dinheiro (precisão além do `float`), decimais, datas, CNPJ,
  idempotência, parsing de problem details, cliente HTTP (headers, erros, abort);
- painel: **uma** requisição de simulação com debounce e o payload correto, nenhuma chamada com
  formulário inválido, 422 inline, adicionar ao lote, trava da moeda de pagamento, fluxo completo da
  página;
- lote: `Idempotency-Key` reutilizada após falha de rede, corpo exato, erros por recebível;
- operação: `If-Match` a partir do ETag, 409 e 412 com recarga, cancelamento;
- extrato: parâmetros iniciais, filtros/página/ordenação/tamanho → requisição **e** URL, restauração
  a partir da URL, gaveta, vazio e erro;
- câmbio: taxa desatualizada, derivada, 404, sincronização (503 amigável e sucesso), cadastro manual,
  histórico paginado; saldos.

Os handlers MSW (`src/test/handlers`) seguem o contrato de `docs/api-contract.md`; requisições sem
handler fazem o teste falhar. O MSW é usado **apenas** nos testes.

## Limitações conhecidas

- Não há autenticação (o backend não a define); em produção o SPA ficaria atrás do mesmo proxy com
  SSO.
- Sem testes E2E em navegador (Playwright/Cypress não foram adicionados por restrição de disco); os
  fluxos foram verificados manualmente contra o backend real.
- O dataset de demonstração do backend contém CNPJs com dígitos verificadores inválidos (inseridos
  via SQL); eles são exibidos normalmente, mas não poderiam ser cadastrados pela API.
