import type { components } from './schema';

/**
 * Contract types used by the application, aliased from the types generated from the backend
 * OpenAPI document (`npm run gen:api` → `schema.d.ts`).
 *
 * The document states the real contract: every response field is required and only the ones the
 * backend may send as `null` are nullable (e.g. `settledAt`, `exchangeRate`). Features import from
 * here, never from `schema.d.ts`, so a backend change surfaces as a type error in one place.
 */
type Schemas = components['schemas'];

/** Decimal carried as a string to keep exact precision, e.g. `"9285.99"` or `"5.13220000"`. */
export type DecimalString = string;
/** ISO-8601 calendar date, e.g. `"2026-12-22"`. */
export type IsoDate = string;
/** ISO-8601 UTC instant, e.g. `"2026-09-23T14:05:00Z"`. */
export type IsoDateTime = string;

export type CurrencyCode = Schemas['SimulationRequest']['faceCurrency'];
export const CURRENCY_CODES = ['BRL', 'USD'] as const satisfies readonly CurrencyCode[];

export type ReceivableTypeCode = Schemas['ReceivableTypeResponse']['code'];

export type OperationStatus = Schemas['CreditAssignmentResponse']['status'];
export const OPERATION_STATUSES = [
  'PENDING',
  'SETTLED',
  'CANCELLED',
] as const satisfies readonly OperationStatus[];

export type ExchangeRateSource = Schemas['ExchangeRateResponse']['source'];

// --- reference data --------------------------------------------------------------------------------

export type Currency = Schemas['CurrencyResponse'];
export type ReceivableType = Schemas['ReceivableTypeResponse'];

// --- exchange rates --------------------------------------------------------------------------------

export type ExchangeRate = Schemas['ExchangeRateResponse'];
export type ExchangeRateSnapshot = Schemas['ExchangeRateSnapshot'];
export type ManualExchangeRateRequest = Schemas['ManualExchangeRateRequest'];

// --- pricing ---------------------------------------------------------------------------------------

export type SimulationRequest = Schemas['SimulationRequest'];
/** `exchangeRate` is `null` when the face currency is the payment currency. */
export type Simulation = Schemas['SimulationResponse'];

// --- assignors -------------------------------------------------------------------------------------

export type Assignor = Schemas['AssignorResponse'];
export type AssignorSummary = Schemas['AssignorSummary'];
export type AssignorRequest = Schemas['AssignorRequest'];

// --- credit assignments ----------------------------------------------------------------------------

export type CreateCreditAssignmentRequest = Schemas['CreateCreditAssignmentRequest'];
export type ReceivableRequest = Schemas['ReceivableRequest'];
export type AssignedReceivable = Schemas['ReceivableResponse'];
export type CreditAssignment = Schemas['CreditAssignmentResponse'];

// --- treasury --------------------------------------------------------------------------------------

export type CashAccount = Schemas['CashAccountResponse'];

// --- settlement statement --------------------------------------------------------------------------

export type StatementItem = Schemas['SettlementStatementItem'];

export type StatementSortField = 'createdAt' | 'settledAt' | 'totalNetAmount' | 'assignorName';

// --- pagination ------------------------------------------------------------------------------------

export type PageMetadata = Schemas['PageMetadata'];

export interface Page<T> {
  content: T[];
  page: PageMetadata;
}
