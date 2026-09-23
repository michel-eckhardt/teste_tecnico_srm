import { describe, expect, it } from 'vitest';

import { createReceivableSchema, toSimulationRequest } from './receivable-form';

const TODAY = '2026-09-23';
const valid = {
  receivableType: 'DUPLICATA_MERCANTIL',
  faceValue: '10000.00',
  faceCurrency: 'BRL',
  dueDate: '2026-12-22',
  paymentCurrency: 'USD',
};

function messagesFor(values: object) {
  const result = createReceivableSchema(TODAY).safeParse({ ...valid, ...values });
  return result.success ? [] : result.error.issues.map((issue) => issue.message);
}

describe('receivable form validation', () => {
  it('builds the simulation request from valid values', () => {
    expect(toSimulationRequest(valid, TODAY)).toEqual(valid);
  });

  it('returns no request while the form is incomplete', () => {
    expect(toSimulationRequest({ ...valid, dueDate: '' }, TODAY)).toBeNull();
    expect(toSimulationRequest({ ...valid, receivableType: '' }, TODAY)).toBeNull();
    expect(toSimulationRequest({}, TODAY)).toBeNull();
  });

  it('requires a positive face value within the backend limit', () => {
    expect(messagesFor({ faceValue: '' })).toEqual(['Informe o valor de face.']);
    expect(messagesFor({ faceValue: '0.00' })).toEqual(['O valor deve ser maior que zero.']);
    expect(messagesFor({ faceValue: '1000000000.00' })).toEqual([]);
    expect(messagesFor({ faceValue: '1000000000.01' })).toEqual([
      'O valor máximo por recebível é 1.000.000.000,00.',
    ]);
    expect(messagesFor({ faceValue: '12,50' })).toEqual(['Valor inválido.']);
  });

  it('accepts due dates from tomorrow up to the 5-year limit', () => {
    expect(messagesFor({ dueDate: TODAY })).toEqual(['O vencimento deve ser posterior a hoje.']);
    expect(messagesFor({ dueDate: '2026-09-24' })).toEqual([]);
    expect(messagesFor({ dueDate: '2031-09-22' })).toEqual([]);
    expect(messagesFor({ dueDate: '2031-09-23' })).toEqual(['O prazo máximo é de 5 anos.']);
    expect(messagesFor({ dueDate: '2026-02-30' })).toEqual(['Data inválida.']);
  });

  it('only accepts supported currencies', () => {
    expect(messagesFor({ faceCurrency: 'EUR' })).toHaveLength(1);
  });
});
