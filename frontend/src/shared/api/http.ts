import createClient, { type Middleware } from 'openapi-fetch';

import { ApiError } from './api-error';
import { CORRELATION_ID_HEADER, networkError, problemToApiError } from './problem';
import type { paths } from './schema';

export const IDEMPOTENCY_KEY_HEADER = 'Idempotency-Key';
export const IF_MATCH_HEADER = 'If-Match';
export const ETAG_HEADER = 'ETag';

/**
 * Every request carries its own correlation id: the backend reuses it (logs, problem details), so
 * even a request that never got an answer can be traced from the id shown to the operator.
 */
const correlation: Middleware = {
  onRequest({ request }) {
    if (!request.headers.has(CORRELATION_ID_HEADER)) {
      request.headers.set(CORRELATION_ID_HEADER, crypto.randomUUID());
    }
    return request;
  },
};

/**
 * Typed HTTP client generated from the backend OpenAPI document (`npm run gen:api`). Paths already
 * contain `/api/v1`; requests always go to the SPA's own origin, where the Vite dev server (or nginx
 * in production) proxies `/api` to the backend.
 */
export const apiClient = createClient<paths>({
  baseUrl: globalThis.location.origin,
  headers: { Accept: 'application/json, application/problem+json' },
  // Resolved at call time (not captured at import time), so test interceptors can patch fetch.
  fetch: (request) => globalThis.fetch(request),
});
apiClient.use(correlation);

/** Successful response: the (contract-refined) body plus the raw response for headers (ETag...). */
export interface ApiResponse<T> {
  data: T;
  response: Response;
}

type ClientResult<T> =
  | { data: T; error?: never; response: Response }
  | { data?: never; error?: unknown; response: Response };

/**
 * Awaits an openapi-fetch call and turns every failure into an {@link ApiError}: problem details for
 * HTTP errors, `NETWORK_ERROR` when there was no response. Aborts (query cancellation) are rethrown
 * untouched so TanStack Query can recognise them.
 */
export async function send<T>(call: Promise<ClientResult<T>>): Promise<ApiResponse<T>> {
  let result: ClientResult<T>;
  try {
    result = await call;
  } catch (cause) {
    if (isAbort(cause) || cause instanceof ApiError) throw cause;
    throw networkError(cause);
  }
  const { response } = result;
  if (!response.ok) {
    throw problemToApiError(result.error, response);
  }
  return { data: result.data as T, response };
}

/** Same as {@link send} for callers that only need the body. */
export async function sendForData<T>(call: Promise<ClientResult<T>>): Promise<T> {
  return (await send(call)).data;
}

/** Duck-typed: `instanceof DOMException` fails across realms (e.g. jsdom vs Node globals). */
function isAbort(error: unknown): boolean {
  return (
    typeof error === 'object' && error !== null && 'name' in error && error.name === 'AbortError'
  );
}
