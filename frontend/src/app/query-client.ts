import { MutationCache, QueryClient } from '@tanstack/react-query';

import { isApiError } from '@/shared/api/api-error';
import { notifyError } from '@/shared/ui/notify';

declare module '@tanstack/react-query' {
  interface Register {
    mutationMeta: {
      /** Set to false when the caller renders the error itself (inline, next to the action). */
      notifyOnError?: boolean;
    };
  }
}

const MAX_RETRIES = 2;

/**
 * Only transient failures are retried: no response at all, or 5xx. A 4xx means the request itself
 * is wrong (or the business rule said no) and would fail again.
 */
export function shouldRetry(failureCount: number, error: unknown): boolean {
  if (failureCount >= MAX_RETRIES) return false;
  if (!isApiError(error)) return false;
  return error.status === 0 || error.status >= 500;
}

export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        retry: shouldRetry,
      },
      mutations: {
        // Commands are never retried automatically: the operator decides (idempotency keys and
        // If-Match make a manual retry safe).
        retry: false,
      },
    },
    mutationCache: new MutationCache({
      onError: (error, _variables, _context, mutation) => {
        if (mutation.meta?.notifyOnError !== false) {
          notifyError(error);
        }
      },
    }),
  });
}
