# Uso de IA no desenvolvimento

Usei o **Claude Code** (modelo Claude Opus) como co-piloto em todo o projeto. A IA escreveu a maior parte do código sob a minha direção. As decisões de arquitetura, o escopo, as revisões e a aprovação de cada fase foram minhas. Este documento registra como a IA foi usada, onde errou e o que aprendi.

## 1. Como o trabalho foi organizado

| Fase | O que eu decidi / fiz | O que a IA fez |
|---|---|---|
| **Planejamento** | Stack (Java 21 + Spring Boot 4 + PostgreSQL, React), escopo (Sênior + ADRs), câmbio pela API Frankfurter, Husky para hooks, execução **sequencial** (backend → frontend → restante) com minha aprovação entre as fases. | Leu o desafio, verificou o ambiente e as versões das bibliotecas no Maven Central/npm, propôs o plano e o contrato da API. |
| **Contrato (API First)** | Aprovei o contrato antes de qualquer código. | Redigiu `docs/api-contract.md` (recursos, payloads, RFC 9457, ETag/If-Match, idempotência). |
| **Backend** | Revisei o código crítico (motor de precificação, liquidação, SQL do extrato, schema), rodei `verify` e testei o fluxo ponta a ponta contra a Frankfurter real. | Um agente implementou 6 branches empilhadas com commits atômicos e testes. |
| **Frontend** | Testei no navegador: simulação, lote, conflito de liquidação, grid sobre 100 mil linhas, câmbio. | Um agente implementou 5 branches empilhadas. |
| **Correções, DevOps e docs** | Direcionei as correções encontradas nas revisões. | Correções, docker compose, Prometheus/Grafana, Husky, CI/CD, diagramas, ADRs e este documento. |

## 2. Prompts estratégicos

Resumo dos prompts que mais influenciaram o resultado (as instruções completas eram mais longas):

1. **Planejamento:** *"Vamos fazer esse desafio técnico para vaga de sênior, os requisitos estão no .md. Quero 1 agente para backend, 1 para frontend, Java 21, Spring 4, Postgres, raiz dividida em backend e frontend."* Seguido de decisões explícitas: execução sequencial, Frankfurter para câmbio, Husky.
2. **Brief do backend (trechos):**
   - listar as armadilhas conhecidas do Spring Boot 4 (starters modulares, `spring-boot-starter-flyway`, Jackson 3 em `tools.jackson`, Testcontainers 2, `@MockitoBean`);
   - impor a política numérica (`BigDecimal` + `DECIMAL128`, `big-math` para potência fracionária, um único `HALF_EVEN`);
   - exigir teste concorrente com `ExecutorService` + `CountDownLatch`;
   - pedir retry **em volta** da transação, nunca dentro;
   - usar whitelist de `ORDER BY`;
   - proibir testes que acessem a rede.
3. **Massa de dados:** *"migração só no perfil demo, com `generate_series`, ~100 mil operações com 1 a 3 recebíveis, distribuição determinística (sem `random()`), totais consistentes com a fórmula, rodando em segundos"*.
4. **Brief do frontend (trechos):**
   - separar estado do servidor (TanStack Query) de estado global do cliente (Zustand);
   - dinheiro sempre como string;
   - gerar os tipos a partir do OpenAPI real;
   - usar MSW só nos testes;
   - manter filtros do grid na URL;
   - tratar erros por `code`.
5. **Registro de erros:** em todo brief pedi um *"AI issues log: casos concretos em que a primeira tentativa estava errada e como foi detectado e corrigido"*, que é a base da seção 3.
6. **Verificação:** *"suba a aplicação e exercite os endpoints reais"*, *"teste no navegador contra o backend real"*, *"meça o p95 do extrato com 100 mil linhas"*.

## 3. Onde a IA errou (e como foi corrigido)

### Erros de API/versão (alucinação ou conhecimento desatualizado)

| Problema | Como foi detectado | Correção |
|---|---|---|
| Mistura de APIs do Spring Boot 3 com o 4 e do Jackson 2 com o 3 (pacotes, nomes de módulos de teste, atributos do `@Retryable`). | Falhas de compilação/contexto. O agente passou a conferir as classes nos JARs antes de usá-las. | Uso dos artefatos e pacotes corretos do Boot 4/Jackson 3. |
| Serialização de `BigDecimal` como string configurada num `@Configuration` que o `@WebMvcTest` não carrega: `"5.13220000"` virava o número `5.1322`. | Teste de fatia (slice test) do controller. | Passou a ser um `@JacksonComponent`. |
| "Últimas versões" incompatíveis com o Node 22.14 (React Router 8, jsdom 30 e, depois, lint-staged 17). | Erros de `engines` na instalação. | Versões fixadas nas majors compatíveis, documentadas nos commits. |
| Lockfile gerado com `npm install --package-lock-only` rejeitado pelo `npm ci`. | `npm ci --dry-run` em cada commit. | Três commits corrigidos com rebase interativo. |
| APIs do Mantine 9 diferentes do esperado (`gutter` → `gap`, selects como `combobox`). | Testes e inspeção visual. | Ajuste dos componentes e das queries de teste. |

### Erros de lógica e de domínio

| Problema | Como foi detectado | Correção |
|---|---|---|
| Taxa `SEED` datada em UTC (`CURRENT_DATE`): data errada depois das 21h em São Paulo. | Revisão do agente. | Data de negócio `America/Sao_Paulo`. |
| Ordem de flush do Hibernate: uma liquidação duplicada batia no índice único do ledger (409 genérico) em vez de acusar conflito de versão. | Teste concorrente. | Flush explícito da operação antes do débito da conta-caixa. |
| Métrica `..._created_total` renomeada silenciosamente pelo cliente Prometheus (sufixo reservado). | Teste de integração de métricas. | Métrica renomeada para `opened`. |
| Massa demo com correlação espúria (toda operação em USD saía liquidada). | Conferência da distribuição. | Aritmética modular corrigida. |
| **A taxa `SEED` "vencia" a cotação real da Frankfurter** num deploy novo: a seed é datada no dia do deploy e o BCE publica o dia útil anterior até a tarde. Os testes passavam porque partiam da mesma premissa. | **Smoke test do docker compose** (a taxa vigente aparecia como `SEED`). | Taxas observadas têm precedência sobre a seed; teste unitário do cenário ([ADR-0008](docs/adr/0008-fx-provider-resilience.md)). |
| **Painéis do Grafana vazios após as primeiras liquidações:** os contadores eram criados no primeiro incremento, e o `rate()`/`increase()` não enxerga essa subida. | **Olhando o dashboard renderizado.** | Séries registradas em zero no startup. A primeira versão da correção criou um ciclo entre módulos de domínio, e o **ArchUnit bloqueou**; as moedas passaram a ser injetadas via `config`. |
| CNPJs da massa demo com dígitos verificadores arbitrários (o comentário dizia "não são CNPJs reais" em vez de calculá-los). | Relato do agente do frontend: a API rejeitava o CNPJ de um cedente demo. | Dígitos calculados em SQL (módulo 11). |

### Contornos em vez de correção na causa raiz

- **OpenAPI incompleto:** todos os campos de resposta apareciam como opcionais, nada como nulo, e os erros e o `201` não estavam documentados. O agente do frontend **contornou** o problema com uma camada de tipos "refinados" em vez de apontar a causa. Corrigi no backend (conversor de schema + `@ProblemResponses` + teste do documento OpenAPI) e removi a camada do frontend.
- **Na minha própria correção**, a primeira versão marcou os campos anuláveis como opcionais (`settledAt?: string | null`). Detectei ao inspecionar os tipos TypeScript gerados e corrigi com `amend!` + autosquash, porque o backend sempre envia `null` explicitamente.

### Problemas de teste e de ferramenta

- Testes com condição de corrida contra o debounce e um clique dentro de `waitFor`: reescritos.
- Um `sed` transformou `/\d/` em `/d/` no validador de CNPJ; um heredoc colapsou `\\` e gerou regex inválida. Nos dois casos a correção foi editar os arquivos diretamente em vez de usar substituição via shell.
- Badges de status cortados no grid em 1024 px: só apareceu no navegador.

### Segurança

Não identifiquei código inseguro gerado neste projeto. Verifiquei ativamente os pontos em que a IA costuma errar:
- concatenação de entrada no `ORDER BY`: evitada com whitelist, e há teste com tentativa de injeção;
- stack trace em respostas 500: há teste que garante que não vaza;
- Postgres exposto no compose: não é publicado;
- containers como root: não são;
- CORS aberto: é restrito às origens configuradas.

A ausência de autenticação é uma decisão de escopo documentada, não um descuido.

## 4. Análise crítica

**Onde a IA economizou tempo**
- Scaffolding e configuração (Spring Boot 4, Flyway, Testcontainers, Vite/ESLint/Vitest, Dockerfiles, compose, CI) que levariam dias foram feitos em horas.
- Testes volumosos e sistemáticos: parametrizados com valores exatos, concorrência, WireMock e MSW.
- Massa de dados em SQL, DDL comentado, dashboard como código e documentação.
- Aplicação disciplinada de padrões pedidos explicitamente (Strategy, ProblemDetail, ETag/If-Match, idempotência).

**Onde a IA atrapalhou**
- **Confiança excessiva em versões e APIs:** assumir "a última versão" ou a API de uma major anterior gerou retrabalho. Especificar as versões no prompt e verificar no JAR reduziu isso.
- **Contornar em vez de corrigir:** diante de um problema em outra camada, a tendência foi compensar localmente (camada de tipos no frontend) em vez de corrigir a origem.
- **Bugs que os testes não pegam:** quando código e teste nascem da mesma premissa errada, os dois concordam. Os três bugs mais relevantes (seed, contadores, dashboard) passaram por mais de 250 testes verdes e só apareceram na **verificação ponta a ponta**: compose, navegador e dashboard.
- **Volume:** o gargalo deixa de ser escrever e passa a ser **revisar**. Commits atômicos e branches pequenas foram essenciais para revisar com atenção.

**O que funcionou como proteção**
- Barreiras automáticas que não dependem de a IA "lembrar": ArchUnit (pegou o ciclo), tipos gerados do OpenAPI, CHECKs no banco, commitlint, Spotless e o gate de cobertura.
- Checkpoints com verificação real (subir a stack, clicar, olhar o dashboard) em vez de confiar em "todos os testes passaram".
- Pedir à IA que registrasse os próprios erros tornou esta análise honesta e rastreável.

## 5. Autoria

Revisei e entendo todo o código entregue: a fórmula e a política de arredondamento, o modelo de concorrência (flush ordenado, retry em volta da transação, ETag/If-Match), a resiliência da integração, as consultas do extrato e a arquitetura do frontend. As decisões e seus trade-offs estão nos [ADRs](docs/adr/README.md). Onde a IA errou, a correção e o motivo estão registrados aqui e nos commits.
