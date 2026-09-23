# Contrato da API — SRM Credit Engine (v1)

Contrato definido **antes** da implementação (API First). A especificação OpenAPI gerada pelo backend (`/v3/api-docs`, Swagger UI em `/swagger-ui.html`) é a fonte de verdade em runtime e deve permanecer aderente a este documento.

## Convenções

- **Base path:** `/api/v1`
- **Formato:** `application/json` (UTF-8). Nomes de campos em `camelCase`.
- **Valores monetários e taxas trafegam como _string_ decimal** (ex.: `"9285.99"`, `"0.01500000"`), nunca como `number`, para evitar perda de precisão em clientes JavaScript (IEEE 754).
  - Valores monetários: escala 2. Taxas: até 8 casas decimais.
- **Datas:** ISO-8601. `LocalDate` (`"2026-12-31"`) para vencimentos/datas de referência; `OffsetDateTime` em UTC (`"2026-09-23T14:05:00Z"`) para timestamps.
- **Moedas:** ISO-4217 (`BRL`, `USD`).
- **Paginação:** query params `page` (base 0) e `size` (padrão 20, máx. 100). Resposta paginada:
  ```json
  { "content": [ ... ], "page": { "number": 0, "size": 20, "totalElements": 1234, "totalPages": 62 } }
  ```
- **Correlação:** header `X-Correlation-Id` é aceito na requisição (ou gerado) e sempre devolvido na resposta; também aparece nos logs e nos erros.
- **Concorrência otimista:** recursos versionados devolvem `ETag: "<version>"`; comandos que alteram estado exigem `If-Match: "<version>"`.
- **Idempotência:** `POST /credit-assignments` aceita header `Idempotency-Key` (UUID recomendado).

## Erros — RFC 9457 (`application/problem+json`)

```json
{
  "type": "https://srm.com.br/problems/insufficient-funds",
  "title": "Saldo insuficiente",
  "status": 422,
  "detail": "Saldo em USD insuficiente para liquidar a operação 0192...",
  "instance": "/api/v1/credit-assignments/0192.../settlement",
  "code": "INSUFFICIENT_FUNDS",
  "correlationId": "5c1f0c1e-...",
  "errors": [ { "field": "receivables[0].faceValue", "message": "deve ser maior que 0" } ]
}
```

| Status | Uso |
|---|---|
| 400 `VALIDATION_ERROR` / `MALFORMED_REQUEST` | payload inválido, campos obrigatórios, formato |
| 404 `RESOURCE_NOT_FOUND` | recurso inexistente |
| 409 `CONFLICT` / `OPERATION_ALREADY_SETTLED` / `INVALID_STATE_TRANSITION` / `CONCURRENT_MODIFICATION` / `DUPLICATE_ASSIGNOR` / `IDEMPOTENCY_KEY_REUSED` | conflito de estado |
| 405 `METHOD_NOT_ALLOWED` / 406 `NOT_ACCEPTABLE` / 415 `UNSUPPORTED_MEDIA_TYPE` | erros de protocolo HTTP (mesmo formato de problema) |
| 412 `PRECONDITION_FAILED` | `If-Match` não corresponde à versão atual |
| 422 `INSUFFICIENT_FUNDS` / `EXCHANGE_RATE_UNAVAILABLE` / `EXCHANGE_RATE_STALE` / `UNSUPPORTED_RECEIVABLE_TYPE` / `INVALID_DUE_DATE` | regra de negócio violada |
| 428 `PRECONDITION_REQUIRED` | `If-Match` ausente |
| 500 `INTERNAL_ERROR` | erro inesperado (sem stack trace na resposta) |
| 503 `FX_PROVIDER_UNAVAILABLE` | provedor de câmbio (Frankfurter) indisponível / circuit breaker aberto |

## Recursos

### Moedas
`GET /currencies` → `200`
```json
[ { "code": "BRL", "name": "Real brasileiro", "decimals": 2 }, { "code": "USD", "name": "Dólar americano", "decimals": 2 } ]
```

### Tipos de recebível
`GET /receivable-types` → `200` (spread vem da Strategy de cada tipo)
```json
[ { "code": "DUPLICATA_MERCANTIL", "description": "Duplicata Mercantil", "monthlySpread": "0.01500000" },
  { "code": "CHEQUE_PRE_DATADO", "description": "Cheque Pré-datado", "monthlySpread": "0.02500000" } ]
```

### Câmbio
Convenção: `rate` = quantas unidades de `quote` valem 1 unidade de `base` (ex.: `USD/BRL = 5.1322`). A conversão inversa é derivada.

`GET /exchange-rates/latest?base=USD&quote=BRL` → `200` | `404`
```json
{ "id": "0192...", "base": "USD", "quote": "BRL", "rate": "5.13220000", "source": "FRANKFURTER",
  "referenceDate": "2026-09-23", "createdAt": "2026-09-23T14:05:00Z", "stale": false }
```
`source`: `MANUAL` | `FRANKFURTER` | `SEED`. `stale = true` quando a taxa é mais antiga que o limite configurado.

`GET /exchange-rates?base=USD&quote=BRL&page=0&size=20` → `200` página do histórico (mais recente primeiro).

`POST /exchange-rates` (atualização manual) → `201` + `Location` | `400`
```json
{ "base": "USD", "quote": "BRL", "rate": "5.2000", "referenceDate": "2026-09-23" }
```
`referenceDate` opcional (padrão: data de negócio atual). `base != quote`, `rate > 0`.

`POST /exchange-rates/sync` → `200` | `503`
Busca as taxas vigentes na API Frankfurter (com retry + circuit breaker) e persiste. Resposta: lista de taxas gravadas (mesmo formato de `latest`).

### Simulação de precificação
`POST /pricing/simulations` → `200` | `400` | `422` (não persiste nada)
```json
{ "receivableType": "DUPLICATA_MERCANTIL", "faceValue": "10000.00", "faceCurrency": "BRL",
  "dueDate": "2026-12-22", "paymentCurrency": "USD" }
```
Resposta:
```json
{
  "receivableType": "DUPLICATA_MERCANTIL",
  "faceValue": "10000.00", "faceCurrency": "BRL", "paymentCurrency": "USD",
  "operationDate": "2026-09-23", "dueDate": "2026-12-22",
  "termDays": 90, "termMonths": "3.00000000",
  "baseRate": "0.01000000", "spread": "0.01500000", "discountRate": "0.02500000",
  "presentValue": "9285.99", "discount": "714.01",
  "exchangeRate": { "id": "0192...", "base": "USD", "quote": "BRL", "rate": "5.13220000", "referenceDate": "2026-09-23" },
  "netAmount": "1809.36"
}
```
Fórmula: `presentValue = faceValue / (1 + baseRate + spread) ^ termMonths`, `termMonths = termDays / 30`. Conversão cambial aplicada **no final** (`netAmount` na moeda de pagamento). `exchangeRate` é `null` quando `faceCurrency == paymentCurrency`.

### Cedentes
`GET /assignors?search=acme&page=0&size=20` → `200` página de `{ "id", "name", "document", "createdAt" }`
`POST /assignors` → `201` + `Location` | `400` (CNPJ inválido) | `409` (`DUPLICATE_ASSIGNOR`)
```json
{ "name": "ACME Indústria Ltda", "document": "11222333000181" }
```
`GET /assignors/{id}` → `200` | `404`

### Operações de cessão (lote de recebíveis)
`POST /credit-assignments` — header `Idempotency-Key` → `201` + `Location` + `ETag` | `400` | `404` (cedente) | `409` (`IDEMPOTENCY_KEY_REUSED` com payload diferente) | `422`
Repetir a mesma chave com o mesmo payload devolve a operação já criada (`200`).
```json
{
  "assignorId": "0192...",
  "paymentCurrency": "BRL",
  "receivables": [
    { "receivableType": "DUPLICATA_MERCANTIL", "faceValue": "10000.00", "faceCurrency": "BRL", "dueDate": "2026-12-22" },
    { "receivableType": "CHEQUE_PRE_DATADO", "faceValue": "2500.00", "faceCurrency": "USD", "dueDate": "2026-11-10" }
  ]
}
```
`receivables`: 1..500 itens.

Resposta (`CreditAssignment`), também em `GET /credit-assignments/{id}` (com `ETag`) — totais ilustrativos:
```json
{
  "id": "0192...", "status": "PENDING", "version": 0,
  "assignor": { "id": "0192...", "name": "ACME Indústria Ltda", "document": "11222333000181" },
  "paymentCurrency": "BRL",
  "totalFaceValue": "22830.50", "totalDiscount": "1453.20", "totalNetAmount": "21377.30",
  "receivablesCount": 2,
  "receivables": [
    { "id": "0192...", "receivableType": "DUPLICATA_MERCANTIL", "faceValue": "10000.00", "faceCurrency": "BRL",
      "dueDate": "2026-12-22", "termDays": 90, "baseRate": "0.01000000", "spread": "0.01500000",
      "presentValue": "9285.99", "discount": "714.01", "exchangeRate": null, "netAmount": "9285.99" }
  ],
  "createdAt": "2026-09-23T14:05:00Z", "settledAt": null
}
```
`status`: `PENDING` | `SETTLED` | `CANCELLED`. Totais expressos na moeda de pagamento.

`POST /credit-assignments/{id}/settlement` — header `If-Match: "<version>"` **obrigatório**
→ `200` (operação `SETTLED`, novo `ETag`) | `404` | `409` (já liquidada/cancelada, conflito concorrente) | `412` | `422` (`INSUFFICIENT_FUNDS`) | `428`
A liquidação debita a conta-caixa do fundo na moeda de pagamento e registra o movimento — tudo na mesma transação.

`POST /credit-assignments/{id}/cancellation` — header `If-Match` obrigatório → `200` (`CANCELLED`) | `404` | `409` | `412` | `428`

### Contas-caixa do fundo
`GET /cash-accounts` → `200`
```json
[ { "currency": "BRL", "balance": "50000000.00", "updatedAt": "2026-09-23T14:05:00Z" },
  { "currency": "USD", "balance": "10000000.00", "updatedAt": "2026-09-23T14:05:00Z" } ]
```

### Extrato de liquidação / histórico de transações (consulta analítica)
`GET /reports/settlement-statement` → `200` | `400`

| Param | Tipo | Descrição |
|---|---|---|
| `from`, `to` | `LocalDate` | período (inclusivo) sobre a data da operação (`createdAt`, fuso de negócio) |
| `assignorId` | UUID | cedente |
| `currency` | `BRL`/`USD` | moeda de pagamento |
| `status` | `PENDING`/`SETTLED`/`CANCELLED` | opcional |
| `page`, `size` | int | paginação server-side |
| `sort` | string | `campo,direção`; campos permitidos: `createdAt`, `settledAt`, `totalNetAmount`, `assignorName` (whitelist) |

```json
{
  "content": [
    { "operationId": "0192...", "assignorId": "0192...", "assignorName": "ACME Indústria Ltda", "assignorDocument": "11222333000181",
      "status": "SETTLED", "paymentCurrency": "BRL", "receivablesCount": 2,
      "totalFaceValue": "22830.50", "totalDiscount": "1453.20", "totalNetAmount": "21377.30",
      "createdAt": "2026-09-23T14:05:00Z", "settledAt": "2026-09-23T14:06:10Z" }
  ],
  "page": { "number": 0, "size": 20, "totalElements": 1, "totalPages": 1 }
}
```
