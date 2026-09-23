import { describe, expect, it } from 'vitest';

import { ApiError, type ApiErrorCode } from '@/shared/api/api-error';

import { explainCommandError } from './command-errors';
import { describeFieldPath, explainSubmissionError } from './submission-errors';

const apiError = (code: ApiErrorCode, status = 409) =>
  new ApiError({ status, code, title: 'Título do servidor', detail: 'Detalhe do servidor' });

describe('explainCommandError', () => {
  it('asks for a reload when the operation changed or is already final', () => {
    expect(explainCommandError(apiError('PRECONDITION_FAILED', 412), 'settle')).toMatchObject({
      title: 'Operação alterada por outra pessoa',
      refetch: true,
    });
    expect(explainCommandError(apiError('OPERATION_ALREADY_SETTLED'), 'settle')).toMatchObject({
      title: 'Operação já liquidada',
      refetch: true,
    });
    expect(explainCommandError(apiError('INVALID_STATE_TRANSITION'), 'cancel').refetch).toBe(true);
    expect(explainCommandError(apiError('CONCURRENT_MODIFICATION'), 'settle').refetch).toBe(true);
  });

  it('keeps the server detail for business rejections that need no reload', () => {
    const explanation = explainCommandError(apiError('INSUFFICIENT_FUNDS', 422), 'settle');
    expect(explanation).toEqual({ title: 'Saldo insuficiente no caixa do fundo', refetch: false });
  });

  it('reloads after a network failure, since the outcome is unknown', () => {
    expect(explainCommandError(apiError('NETWORK_ERROR', 0), 'settle').refetch).toBe(true);
  });
});

describe('explainSubmissionError', () => {
  it('tells the operator that retrying after an unanswered request is safe', () => {
    expect(explainSubmissionError(apiError('NETWORK_ERROR', 0)).hint).toMatch(
      /mesma chave de idempotência/,
    );
  });

  it('names receivables in field paths', () => {
    expect(describeFieldPath('receivables[1].faceValue')).toBe('Recebível 2 · faceValue');
    expect(describeFieldPath('receivables[0]')).toBe('Recebível 1');
    expect(describeFieldPath('assignorId')).toBe('assignorId');
  });
});
