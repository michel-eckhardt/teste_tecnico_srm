import { describe, expect, it } from 'vitest';

import { createManualRateSchema, formFieldOf, toManualRateRequest } from './manual-rate-form';

const TODAY = '2026-09-23';
const valid = { base: 'USD', quote: 'BRL', rate: '5,2', referenceDate: '' } as const;

function messages(values: object) {
  const result = createManualRateSchema(TODAY).safeParse({ ...valid, ...values });
  return result.success ? [] : result.error.issues.map((issue) => issue.message);
}

describe('manual rate form', () => {
  it('sends the rate as an exact decimal string and omits an empty reference date', () => {
    expect(toManualRateRequest(valid)).toEqual({ base: 'USD', quote: 'BRL', rate: '5.2' });
    expect(
      toManualRateRequest({ ...valid, rate: '0.19484821', referenceDate: '2026-09-22' }),
    ).toEqual({ base: 'USD', quote: 'BRL', rate: '0.19484821', referenceDate: '2026-09-22' });
  });

  it('validates the rate like the backend', () => {
    expect(messages({})).toEqual([]);
    expect(messages({ rate: '' })).toEqual(['Informe a taxa.']);
    expect(messages({ rate: '0' })).toEqual(['A taxa deve ser maior que zero.']);
    expect(messages({ rate: '5.123456789' })).toEqual([
      'Use um número com até 11 dígitos inteiros e 8 casas decimais.',
    ]);
    expect(messages({ rate: 'abc' })).toHaveLength(1);
  });

  it('requires distinct currencies and a reference date not in the future', () => {
    expect(messages({ quote: 'USD' })).toEqual(['A moeda cotada deve ser diferente da base.']);
    expect(messages({ referenceDate: '2026-09-24' })).toEqual(['A data não pode estar no futuro.']);
    expect(messages({ referenceDate: TODAY })).toEqual([]);
  });

  it('maps server field names onto the form', () => {
    expect(formFieldOf('distinctCurrencies')).toBe('quote');
    expect(formFieldOf('rate')).toBe('rate');
    expect(formFieldOf('other')).toBeUndefined();
  });
});
