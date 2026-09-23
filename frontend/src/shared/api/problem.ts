import { z } from 'zod';

import { ApiError, isKnownErrorCode, type ApiErrorCode } from './api-error';

export const CORRELATION_ID_HEADER = 'X-Correlation-Id';

/**
 * RFC 9457 problem details as emitted by the backend (`application/problem+json`). Parsed
 * defensively: a proxy or a crashed server may answer with HTML or an empty body instead.
 */
const problemSchema = z.looseObject({
  type: z.string().optional(),
  title: z.string().optional(),
  status: z.number().int().optional(),
  detail: z.string().optional(),
  instance: z.string().optional(),
  code: z.string().optional(),
  correlationId: z.string().optional(),
  errors: z.array(z.object({ field: z.string(), message: z.string() })).optional(),
});

const FALLBACK_BY_STATUS: Record<number, { code: ApiErrorCode; title: string; detail: string }> = {
  400: {
    code: 'VALIDATION_ERROR',
    title: 'Requisição inválida',
    detail: 'Verifique os dados informados.',
  },
  404: {
    code: 'RESOURCE_NOT_FOUND',
    title: 'Recurso não encontrado',
    detail: 'O recurso solicitado não existe.',
  },
  409: { code: 'CONFLICT', title: 'Conflito', detail: 'A operação conflita com o estado atual.' },
  412: {
    code: 'PRECONDITION_FAILED',
    title: 'Versão desatualizada',
    detail: 'O recurso foi alterado por outra pessoa. Recarregue e tente novamente.',
  },
  428: {
    code: 'PRECONDITION_REQUIRED',
    title: 'Pré-condição obrigatória',
    detail: 'A versão do recurso não foi informada.',
  },
  503: {
    code: 'INTERNAL_ERROR',
    title: 'Serviço indisponível',
    detail: 'O serviço está temporariamente indisponível. Tente novamente em instantes.',
  },
};

function fallbackFor(status: number) {
  const known = FALLBACK_BY_STATUS[status];
  if (known) return known;
  if (status >= 500) {
    return {
      code: 'INTERNAL_ERROR' as const,
      title: 'Erro interno',
      detail: 'Ocorreu um erro inesperado no servidor. Tente novamente em instantes.',
    };
  }
  return {
    code: 'UNEXPECTED_RESPONSE' as const,
    title: 'Resposta inesperada',
    detail: `O servidor respondeu com um status inesperado (${status}).`,
  };
}

/**
 * Builds the ApiError of a failed response from its (already read) body. Server messages are kept
 * as they are (they are written for the operator, in Portuguese); unknown or missing codes fall
 * back to a code derived from the HTTP status, so the UI can always branch on `code`.
 */
export function problemToApiError(body: unknown, response: Response): ApiError {
  const parsed = problemSchema.safeParse(body);
  const problem = parsed.success ? parsed.data : undefined;
  const fallback = fallbackFor(response.status);
  const code = isKnownErrorCode(problem?.code) ? problem.code : fallback.code;

  return new ApiError({
    status: response.status,
    code,
    title: nonBlank(problem?.title) ?? fallback.title,
    detail: nonBlank(problem?.detail) ?? fallback.detail,
    correlationId:
      nonBlank(problem?.correlationId) ??
      nonBlank(response.headers.get(CORRELATION_ID_HEADER)) ??
      undefined,
    fieldErrors: problem?.errors ?? [],
    instance: problem?.instance,
  });
}

/** Failure without an HTTP response (offline, DNS, CORS, server down behind the proxy...). */
export function networkError(cause: unknown, correlationId?: string): ApiError {
  return new ApiError({
    status: 0,
    code: 'NETWORK_ERROR',
    title: 'Falha de comunicação',
    detail: 'Não foi possível contatar o servidor. Verifique sua conexão e tente novamente.',
    correlationId,
    cause,
  });
}

function nonBlank(value: string | null | undefined): string | undefined {
  return value != null && value.trim() !== '' ? value : undefined;
}
