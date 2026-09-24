# Modelo de dados

- **DDL completo:** [`schema.sql`](schema.sql) (snapshot gerado com `pg_dump` a partir das migrações).
- **Fonte de verdade:** migrações Flyway em [`backend/src/main/resources/db/migration`](../../backend/src/main/resources/db/migration) — `V1` (schema) e `V2` (dados de referência: moedas, tipos de recebível, contas-caixa e uma taxa `SEED`). A massa de demonstração (~100 mil operações) fica em `db/demo` e só roda no perfil `demo`.

## Diagrama ER

```mermaid
erDiagram
  CURRENCY {
    varchar code PK "ISO-4217: BRL, USD"
    varchar name
    smallint decimals "casas usadas no arredondamento"
  }
  EXCHANGE_RATE {
    uuid id PK
    varchar base_currency FK
    varchar quote_currency FK
    numeric rate "NUMERIC(19,8) > 0"
    varchar source "MANUAL | FRANKFURTER | SEED"
    date reference_date "data de referência (ex.: publicação do BCE)"
    timestamptz created_at
  }
  RECEIVABLE_TYPE {
    varchar code PK "DUPLICATA_MERCANTIL, CHEQUE_PRE_DATADO"
    varchar description
    boolean active
  }
  ASSIGNOR {
    uuid id PK
    varchar name
    varchar document UK "CNPJ, 14 dígitos"
    timestamptz created_at
  }
  CREDIT_ASSIGNMENT {
    uuid id PK
    uuid assignor_id FK
    varchar payment_currency FK
    varchar status "PENDING | SETTLED | CANCELLED"
    numeric total_face_value "moeda de pagamento"
    numeric total_discount
    numeric total_net_amount
    int receivables_count
    varchar idempotency_key UK
    varchar request_hash "SHA-256 do payload"
    bigint version "optimistic locking (ETag)"
    timestamptz created_at
    timestamptz settled_at
    timestamptz cancelled_at
  }
  RECEIVABLE {
    uuid id PK
    uuid credit_assignment_id FK
    varchar type_code FK
    numeric face_value "moeda de face"
    varchar face_currency FK
    date due_date
    int term_days
    numeric base_rate "snapshot"
    numeric spread "snapshot da Strategy"
    numeric present_value "moeda de face"
    numeric discount "moeda de face"
    uuid exchange_rate_id FK "null sem conversão"
    numeric exchange_rate_applied "snapshot"
    numeric net_amount "moeda de pagamento"
  }
  FUND_CASH_ACCOUNT {
    varchar currency PK, FK
    numeric balance "CHECK >= 0"
    bigint version "optimistic locking"
    timestamptz updated_at
  }
  CASH_MOVEMENT {
    uuid id PK
    varchar currency FK
    uuid credit_assignment_id FK
    numeric amount
    varchar direction "DEBIT | CREDIT"
    timestamptz created_at
  }

  CURRENCY ||--o{ EXCHANGE_RATE : "base / cotada"
  CURRENCY ||--o{ CREDIT_ASSIGNMENT : "moeda de pagamento"
  CURRENCY ||--o{ RECEIVABLE : "moeda de face"
  CURRENCY ||--o| FUND_CASH_ACCOUNT : "conta-caixa"
  RECEIVABLE_TYPE ||--o{ RECEIVABLE : "tipo (produto)"
  ASSIGNOR ||--o{ CREDIT_ASSIGNMENT : "cede"
  CREDIT_ASSIGNMENT ||--|{ RECEIVABLE : "lote (1..500)"
  EXCHANGE_RATE |o--o{ RECEIVABLE : "taxa aplicada"
  FUND_CASH_ACCOUNT ||--o{ CASH_MOVEMENT : "ledger"
  CREDIT_ASSIGNMENT |o--o| CASH_MOVEMENT : "débito da liquidação"
```

## Como a modelagem reflete o problema financeiro

| Decisão | Por quê |
|---|---|
| **`NUMERIC(19,2)` para valores e `NUMERIC(19,8)` para taxas** (nunca `float`/`double`) | Aritmética decimal exata; o Java usa `BigDecimal` de ponta a ponta e a API trafega valores como string. |
| **Snapshot das taxas em cada recebível** (`base_rate`, `spread`, `exchange_rate_applied`, `exchange_rate_id`) | Auditoria: qualquer operação pode ser recalculada e explicada anos depois, mesmo que spreads e câmbio tenham mudado. |
| **`exchange_rate` append-only** | Histórico imutável de cotações; a "taxa vigente" é a observação mais recente (observadas têm precedência sobre a `SEED`). |
| **Totais denormalizados em `credit_assignment`** | O extrato sobre milhões de linhas não precisa agregar recebíveis; a consistência é garantida pelo CHECK `total_face_value = total_discount + total_net_amount`. |
| **CHECKs de ciclo de vida** (`PENDING` ⇒ sem `settled_at`/`cancelled_at`, etc.) e **`balance >= 0`** | Defesa em profundidade: mesmo um bug na aplicação não consegue gravar um estado financeiro impossível. |
| **`version` em `credit_assignment` e `fund_cash_account`** | Concorrência otimista: duas liquidações simultâneas da mesma operação não podem ambas vencer; a disputa pelo saldo é detectada e repetida. |
| **Índice único parcial `uq_cash_movement_settlement_debit`** (um `DEBIT` por operação) | Garantia no banco de que uma operação nunca é debitada duas vezes. |
| **`idempotency_key` única + `request_hash`** | Reenvio seguro do `POST` de criação (timeouts de rede) sem duplicar a cessão; mesma chave com payload diferente é rejeitada. |
| **UUID v7** | Chaves ordenadas no tempo: boa localidade nos índices B-tree, geradas pela aplicação sem ida ao banco. |
| **`TIMESTAMPTZ` em UTC + data de negócio em `America/Sao_Paulo`** | Instantes sem ambiguidade; filtros de período convertidos para intervalos meio-abertos sobre a coluna indexada. |

## Índices do extrato

| Índice | Atende |
|---|---|
| `ix_credit_assignment_created_at (created_at DESC, id DESC)` | Listagem padrão e paginação por data |
| `ix_credit_assignment_assignor_created_at (assignor_id, created_at DESC)` | Filtro por cedente + período |
| `ix_credit_assignment_currency_created_at (payment_currency, created_at DESC)` | Filtro por moeda + período |
| `ix_credit_assignment_status_created_at (status, created_at DESC)` | Filtro por status + período |

Medições com 100 mil operações estão no [README](../../README.md#desempenho-do-extrato).
