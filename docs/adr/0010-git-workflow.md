# ADR-0010 — GitHub Flow com histórico linear, Conventional Commits e Husky

- **Status:** Aceita
- **Data:** 2026-09-23

## Contexto

O histórico do repositório é avaliado: commits rastreáveis, PRs descritos, sem merges poluídos, hooks, tags semânticas e uso de rebase. O projeto tem uma única linha de produção (sem múltiplas versões mantidas em paralelo).

## Decisão

- **GitHub Flow:** `main` sempre implantável; cada funcionalidade em uma branch curta (`feature/*`, `fix/*`, `chore/*`, `docs/*`) integrada por Pull Request.
- **Histórico linear:** branches rebaseadas sobre `main` e integradas por fast-forward (sem merge commits). Correções durante o desenvolvimento viram `fixup!`/`amend!` e são incorporadas com `rebase -i --autosquash`; commits também foram reordenados para agrupar a lógica.
- **Conventional Commits** validados localmente (hook `commit-msg` com commitlint) e no CI (todos os commits do PR).
- **Husky** (em `frontend/`, único lado com Node) protege o repositório inteiro: `pre-commit` roda lint-staged no frontend e `spotless:check` quando há Java; `pre-push` roda os testes unitários dos dois lados.
- **SemVer com tags anotadas** (`v1.0.0`); a tag dispara a publicação das imagens e da release.

## Alternativas consideradas

- **Git Flow** (`develop`, `release/*`, `hotfix/*`) — útil com várias versões em produção; aqui só adicionaria cerimônia.
- **Trunk-based puro** (commits diretos na `main`) — exige feature flags e maturidade de CI que o desafio não pede; PRs dão o ponto de revisão exigido.
- **pre-commit (Python) / Lefthook** — igualmente válidos; Husky foi escolhido por ser a ferramenta citada e já haver Node no projeto.

## Consequências

- `git log --oneline` conta a história por funcionalidade; cada PR é revisável isoladamente.
- Hooks podem ser contornados localmente (`--no-verify`); o CI repete as mesmas verificações como barreira final.
