import { describe, expect, it } from 'vitest';

import { ApiError, isApiError, toApiError } from './api-error';
import { networkError, problemToApiError } from './problem';

function response(status: number, headers: Record<string, string> = {}) {
  return new Response(null, { status, headers });
}

describe('problemToApiError', () => {
  it('maps RFC 9457 problem details, including field violations and the correlation id', () => {
    const error = problemToApiError(
      {
        type: 'https://srm.com.br/problems/validation-error',
        title: 'Requisição inválida',
        status: 400,
        detail: 'Um ou mais campos são inválidos.',
        instance: '/api/v1/credit-assignments',
        code: 'VALIDATION_ERROR',
        correlationId: '5c1f0c1e',
        errors: [{ field: 'receivables[0].faceValue', message: 'deve ser maior que 0' }],
      },
      response(400),
    );

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      status: 400,
      code: 'VALIDATION_ERROR',
      title: 'Requisição inválida',
      detail: 'Um ou mais campos são inválidos.',
      correlationId: '5c1f0c1e',
      instance: '/api/v1/credit-assignments',
      fieldErrors: [{ field: 'receivables[0].faceValue', message: 'deve ser maior que 0' }],
    });
    expect(error.message).toBe('Um ou mais campos são inválidos.');
  });

  it('keeps business codes so the UI can branch on them', () => {
    const error = problemToApiError(
      {
        status: 409,
        title: 'Operação já liquidada',
        detail: 'A operação já foi liquidada.',
        code: 'OPERATION_ALREADY_SETTLED',
      },
      response(409),
    );
    expect(error.is('OPERATION_ALREADY_SETTLED')).toBe(true);
    expect(isApiError(error, 'OPERATION_ALREADY_SETTLED', 'INVALID_STATE_TRANSITION')).toBe(true);
    expect(isApiError(error, 'PRECONDITION_FAILED')).toBe(false);
  });

  it('falls back to a status-based code and the response header when the body is not a problem', () => {
    const error = problemToApiError(
      '<html>Bad Gateway</html>',
      response(502, { 'X-Correlation-Id': 'from-header' }),
    );
    expect(error).toMatchObject({
      status: 502,
      code: 'INTERNAL_ERROR',
      title: 'Erro interno',
      correlationId: 'from-header',
      fieldErrors: [],
    });
  });

  it('derives a code for unknown server codes and blank messages', () => {
    const error = problemToApiError(
      { code: 'SOMETHING_NEW', title: ' ', detail: '' },
      response(412),
    );
    expect(error.code).toBe('PRECONDITION_FAILED');
    expect(error.title).toBe('Versão desatualizada');
    expect(error.detail).toMatch(/alterado por outra pessoa/);
  });

  it('ignores malformed field violations instead of crashing', () => {
    const error = problemToApiError({ code: 'VALIDATION_ERROR', errors: 'oops' }, response(400));
    expect(error.code).toBe('VALIDATION_ERROR');
    expect(error.fieldErrors).toEqual([]);
  });
});

describe('client-side errors', () => {
  it('describes network failures', () => {
    const error = networkError(new TypeError('Failed to fetch'), 'abc');
    expect(error).toMatchObject({ status: 0, code: 'NETWORK_ERROR', correlationId: 'abc' });
    expect(error.cause).toBeInstanceOf(TypeError);
  });

  it('normalizes unknown errors for display', () => {
    const apiError = networkError(null);
    expect(toApiError(apiError)).toBe(apiError);
    expect(toApiError(new Error('boom'))).toMatchObject({
      code: 'UNEXPECTED_RESPONSE',
      title: 'Erro inesperado',
    });
  });
});
