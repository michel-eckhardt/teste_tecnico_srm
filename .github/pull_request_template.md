## O que muda

<!-- Resumo em 1-3 frases: o problema e a solução. -->

## Como foi feito

<!-- Decisões relevantes, trade-offs, alternativas descartadas. Link para ADR se houver. -->

## Como testar

<!-- Comandos, cenários e dados para o revisor reproduzir. -->

## Checklist

- [ ] Commits no padrão Conventional Commits (validado pelo hook e pelo CI)
- [ ] Testes cobrindo a mudança (unitários e/ou integração)
- [ ] `./mvnw verify` e/ou `npm run lint && npm run typecheck && npm test` verdes
- [ ] Contrato da API (`docs/api-contract.md` / OpenAPI) atualizado, se aplicável
- [ ] Sem dados sensíveis em logs, testes ou commits
