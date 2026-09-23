import { isApiError } from '@/shared/api/api-error';

/**
 * Operator-facing explanation of a failed synchronization. A provider outage is expected and
 * harmless (retries and the circuit breaker already ran on the server; stored rates keep working),
 * so it is explained rather than reported as a failure of the application.
 */
export function explainSyncError(error: unknown): { title: string; message?: string } {
  if (isApiError(error, 'FX_PROVIDER_UNAVAILABLE')) {
    return {
      title: 'Provedor de câmbio indisponível',
      message:
        'A Frankfurter não respondeu ou está temporariamente bloqueada pelo circuit breaker. As taxas já cadastradas continuam valendo; tente novamente em alguns minutos ou cadastre uma taxa manualmente.',
    };
  }
  return { title: 'Não foi possível sincronizar as taxas' };
}
