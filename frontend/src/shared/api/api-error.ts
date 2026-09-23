/**
 * Stable error codes returned by the backend in the `code` member of RFC 9457 problem details
 * (docs/api-contract.md), plus the client-side codes for failures that never reached the API.
 * The UI branches on these codes, never on the (human, Portuguese) messages.
 */
export const API_ERROR_CODES = [
  'VALIDATION_ERROR',
  'MALFORMED_REQUEST',
  'RESOURCE_NOT_FOUND',
  'METHOD_NOT_ALLOWED',
  'NOT_ACCEPTABLE',
  'UNSUPPORTED_MEDIA_TYPE',
  'CONFLICT',
  'CONCURRENT_MODIFICATION',
  'DUPLICATE_ASSIGNOR',
  'OPERATION_ALREADY_SETTLED',
  'INVALID_STATE_TRANSITION',
  'IDEMPOTENCY_KEY_REUSED',
  'PRECONDITION_FAILED',
  'PRECONDITION_REQUIRED',
  'INSUFFICIENT_FUNDS',
  'EXCHANGE_RATE_UNAVAILABLE',
  'EXCHANGE_RATE_STALE',
  'INVALID_DUE_DATE',
  'UNSUPPORTED_RECEIVABLE_TYPE',
  'INTERNAL_ERROR',
  'FX_PROVIDER_UNAVAILABLE',
  // client side
  'NETWORK_ERROR',
  'UNEXPECTED_RESPONSE',
] as const;

export type ApiErrorCode = (typeof API_ERROR_CODES)[number];

export interface FieldViolation {
  /** Request field path as reported by the backend, e.g. `receivables[0].faceValue`. */
  field: string;
  message: string;
}

export interface ApiErrorInit {
  status: number;
  code: ApiErrorCode;
  title: string;
  detail: string;
  correlationId?: string | undefined;
  fieldErrors?: readonly FieldViolation[];
  instance?: string | undefined;
  cause?: unknown;
}

/** Every failed API call is rejected with an ApiError (never a raw Response or TypeError). */
export class ApiError extends Error {
  /** HTTP status; `0` when the request never got a response (network failure). */
  readonly status: number;
  readonly code: ApiErrorCode;
  readonly title: string;
  readonly detail: string;
  /** Id to quote to support: it is in the backend logs of the failed request. */
  readonly correlationId: string | undefined;
  readonly fieldErrors: readonly FieldViolation[];
  readonly instance: string | undefined;

  constructor(init: ApiErrorInit) {
    super(init.detail, { cause: init.cause });
    this.name = 'ApiError';
    this.status = init.status;
    this.code = init.code;
    this.title = init.title;
    this.detail = init.detail;
    this.correlationId = init.correlationId;
    this.fieldErrors = init.fieldErrors ?? [];
    this.instance = init.instance;
  }

  is(...codes: ApiErrorCode[]): boolean {
    return codes.includes(this.code);
  }
}

export function isApiError(error: unknown, ...codes: ApiErrorCode[]): error is ApiError {
  return error instanceof ApiError && (codes.length === 0 || error.is(...codes));
}

/** Normalizes anything thrown (ApiError, programming error...) for display. */
export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error;
  return new ApiError({
    status: 0,
    code: 'UNEXPECTED_RESPONSE',
    title: 'Erro inesperado',
    detail: 'Ocorreu um erro inesperado. Tente novamente.',
    cause: error,
  });
}

export function isKnownErrorCode(value: unknown): value is ApiErrorCode {
  return typeof value === 'string' && (API_ERROR_CODES as readonly string[]).includes(value);
}
