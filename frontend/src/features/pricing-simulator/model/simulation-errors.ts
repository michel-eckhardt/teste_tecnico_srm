import { isApiError } from '@/shared/api/api-error';

/**
 * What the operator can do about a rejected simulation (the server already explains *why* in the
 * problem detail). Branches on the stable error code, never on the message.
 */
export function simulationErrorHint(error: unknown): string | undefined {
  if (!isApiError(error)) return undefined;
  switch (error.code) {
    case 'EXCHANGE_RATE_STALE':
      return 'A cotação disponível está desatualizada para operações entre moedas. Sincronize ou cadastre uma taxa na tela Câmbio.';
    case 'EXCHANGE_RATE_UNAVAILABLE':
      return 'Não há cotação para este par de moedas. Cadastre uma taxa na tela Câmbio.';
    case 'INVALID_DUE_DATE':
      return 'Ajuste a data de vencimento.';
    case 'UNSUPPORTED_RECEIVABLE_TYPE':
      return 'Selecione outro tipo de recebível.';
    default:
      return undefined;
  }
}
