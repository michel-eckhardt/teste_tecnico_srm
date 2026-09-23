import type { components } from './schema';

/**
 * Contract types used by the application, derived from the generated OpenAPI types (`schema.d.ts`).
 *
 * springdoc documents every response property as optional and never as nullable, while the
 * backend always serializes every property (absent values as explicit `null`, see
 * docs/api-contract.md). The types below restore that contract: all response properties are
 * present, and only the ones listed as nullable may be `null`.
 *
 * `Narrows<Strict, Loose>` is a compile-time proof that a refined type only narrows the generated
 * one: regenerating the schema after a backend change (renamed field, new enum value...) breaks the
 * build here instead of failing at runtime.
 */
type Schemas = components['schemas'];

/** The generated type, with `null` accepted anywhere (the schema never declares nullability). */
type Nullish<T> = T extends readonly (infer Item)[]
  ? Nullish<Item>[]
  : T extends object
    ? { [K in keyof T]?: Nullish<T[K]> | null }
    : T;

type Narrows<Strict extends Nullish<Loose>, Loose> = Strict;

/** All properties required and non-undefined; `Nullable` ones may be `null`. */
type Present<T, Nullable extends keyof T = never> = {
  [K in keyof T]-?: K extends Nullable ? Exclude<T[K], undefined> | null : Exclude<T[K], undefined>;
};

/** Decimal carried as a string to keep exact precision, e.g. `"9285.99"` or `"5.13220000"`. */
export type DecimalString = string;
/** ISO-8601 calendar date, e.g. `"2026-12-22"`. */
export type IsoDate = string;
/** ISO-8601 UTC instant, e.g. `"2026-09-23T14:05:00Z"`. */
export type IsoDateTime = string;

export type CurrencyCode = Schemas['SimulationRequest']['faceCurrency'];
export const CURRENCY_CODES = ['BRL', 'USD'] as const satisfies readonly CurrencyCode[];

export type ReceivableTypeCode = NonNullable<Schemas['ReceivableTypeResponse']['code']>;

export type OperationStatus = NonNullable<Schemas['CreditAssignmentResponse']['status']>;
export const OPERATION_STATUSES = [
  'PENDING',
  'SETTLED',
  'CANCELLED',
] as const satisfies readonly OperationStatus[];

export type ExchangeRateSource = NonNullable<Schemas['ExchangeRateResponse']['source']>;

// --- reference data --------------------------------------------------------------------------------

export type Currency = Narrows<Present<Schemas['CurrencyResponse']>, Schemas['CurrencyResponse']>;

export type ReceivableType = Narrows<
  Present<Schemas['ReceivableTypeResponse']>,
  Schemas['ReceivableTypeResponse']
>;

// --- exchange rates --------------------------------------------------------------------------------

export type ExchangeRate = Narrows<
  Present<Schemas['ExchangeRateResponse']>,
  Schemas['ExchangeRateResponse']
>;

export type ExchangeRateSnapshot = Narrows<
  Present<Schemas['ExchangeRateSnapshot']>,
  Schemas['ExchangeRateSnapshot']
>;

export type ManualExchangeRateRequest = Schemas['ManualExchangeRateRequest'];

// --- pricing ---------------------------------------------------------------------------------------

export type SimulationRequest = Schemas['SimulationRequest'];

export type Simulation = Narrows<
  Omit<Present<Schemas['SimulationResponse']>, 'exchangeRate'> & {
    /** `null` when the face currency is the payment currency. */
    exchangeRate: ExchangeRateSnapshot | null;
  },
  Schemas['SimulationResponse']
>;

// --- assignors -------------------------------------------------------------------------------------

export type Assignor = Narrows<Present<Schemas['AssignorResponse']>, Schemas['AssignorResponse']>;
export type AssignorSummary = Narrows<
  Present<Schemas['AssignorSummary']>,
  Schemas['AssignorSummary']
>;
export type AssignorRequest = Schemas['AssignorRequest'];

// --- credit assignments ----------------------------------------------------------------------------

export type CreateCreditAssignmentRequest = Schemas['CreateCreditAssignmentRequest'];
export type ReceivableRequest = Schemas['ReceivableRequest'];

export type AssignedReceivable = Narrows<
  Omit<Present<Schemas['ReceivableResponse']>, 'exchangeRate'> & {
    exchangeRate: ExchangeRateSnapshot | null;
  },
  Schemas['ReceivableResponse']
>;

export type CreditAssignment = Narrows<
  Omit<
    Present<Schemas['CreditAssignmentResponse'], 'settledAt' | 'cancelledAt'>,
    'assignor' | 'receivables'
  > & {
    assignor: AssignorSummary;
    receivables: AssignedReceivable[];
  },
  Schemas['CreditAssignmentResponse']
>;

// --- treasury --------------------------------------------------------------------------------------

export type CashAccount = Narrows<
  Present<Schemas['CashAccountResponse']>,
  Schemas['CashAccountResponse']
>;

// --- settlement statement --------------------------------------------------------------------------

export type StatementItem = Narrows<
  Present<Schemas['SettlementStatementItem'], 'settledAt'>,
  Schemas['SettlementStatementItem']
>;

export type StatementSortField = 'createdAt' | 'settledAt' | 'totalNetAmount' | 'assignorName';

// --- pagination ------------------------------------------------------------------------------------

export type PageMetadata = Narrows<Present<Schemas['PageMetadata']>, Schemas['PageMetadata']>;

export interface Page<T> {
  content: T[];
  page: PageMetadata;
}
