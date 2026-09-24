# ADR-0004 — Strategy por tipo de recebível e política numérica

- **Status:** Aceita
- **Data:** 2026-09-23

## Contexto

`Valor Presente = Valor Face / (1 + Taxa Base + Spread) ^ Prazo`. Cada tipo de recebível tem uma regra de risco (spread) diferente, e novos produtos vão surgir. O prazo em meses é fracionário (ex.: 45 dias = 1,5 mês) e a conversão cambial deve ser aplicada no final.

## Decisão

- **Strategy:** interface `PricingStrategy` (`type()`, `monthlySpread(PricingContext)`), uma implementação por tipo (`DuplicataMercantilPricingStrategy` 1,5% a.m., `ChequePreDatadoPricingStrategy` 2,5% a.m., configuráveis). O `PricingStrategyResolver` indexa todas as strategies num `EnumMap` e **falha no startup** se um tipo não tiver strategy ou tiver duas. Um tipo novo = uma classe nova (OCP); o `PricingEngine` não muda.
- **Precisão:** `BigDecimal` com `MathContext.DECIMAL128` (34 dígitos) em todos os passos; potência fracionária com `big-math` (`BigDecimalMath.pow`), nunca `double`.
- **Prazo:** `dias corridos / 30` a partir da data de negócio em `America/Sao_Paulo`.
- **Câmbio no final:** a conversão é aplicada sobre o valor presente **não arredondado**; um único arredondamento `HALF_EVEN` (bancário) nas casas da moeda. `deságio = face − presente`, então os dois sempre somam o valor de face.
- **Taxa base** por moeda de face (configurável).

## Alternativas consideradas

- **Spread numa tabela do banco sem Strategy** — resolve o número, mas não a regra: tipos futuros podem ter spread por faixa de prazo/valor, que cabem em `monthlySpread(PricingContext)`.
- **`double` + `Math.pow`** — erro de representação inaceitável em valores financeiros.
- **`HALF_UP`** — viés sistemático para cima em grandes volumes; `HALF_EVEN` elimina o viés.

## Consequências

- Resultados exatos e reprodutíveis, cobertos por testes parametrizados com valores calculados à parte (ex.: 10.000,00 BRL, 90 dias, duplicata → 9.285,99).
- Cada recebível guarda o snapshot de taxa base, spread e câmbio usados (auditoria).
