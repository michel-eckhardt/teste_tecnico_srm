import { isApiError } from '@/shared/api/api-error';

export interface SubmissionErrorExplanation {
  title?: string;
  /** Replaces the server detail when the operator needs different guidance. */
  message?: string;
  hint?: string;
}

/**
 * Guidance for a failed batch submission. What matters most: after a failure with no answer, a
 * retry is safe because it reuses the same idempotency key (the server returns the operation it may
 * already have created instead of creating a second one).
 */
export function explainSubmissionError(error: unknown): SubmissionErrorExplanation {
  if (!isApiError(error)) return {};
  switch (error.code) {
    case 'NETWORK_ERROR':
    case 'INTERNAL_ERROR':
      return {
        title: 'Não foi possível confirmar o registro',
        hint: 'Tente novamente: o reenvio usa a mesma chave de idempotência, então a operação nunca é registrada em duplicidade.',
      };
    case 'IDEMPOTENCY_KEY_REUSED':
      return {
        message: 'O lote mudou desde a última tentativa de envio.',
        hint: 'Revise o lote e registre novamente.',
      };
    case 'EXCHANGE_RATE_STALE':
    case 'EXCHANGE_RATE_UNAVAILABLE':
      return { hint: 'Atualize a cotação na tela Câmbio e registre novamente.' };
    case 'INVALID_DUE_DATE':
      return { hint: 'Remova ou corrija o recebível com vencimento inválido.' };
    case 'RESOURCE_NOT_FOUND':
      return { hint: 'Selecione o cedente novamente.' };
    default:
      return {};
  }
}

/** `receivables[1].faceValue` → `Recebível 2 · faceValue`. */
export function describeFieldPath(path: string): string {
  const match = /^receivables\[(\d+)\]\.?(.*)$/.exec(path);
  if (!match) return path;
  const [, index = '0', field = ''] = match;
  return `Recebível ${Number(index) + 1}${field ? ` · ${field}` : ''}`;
}
