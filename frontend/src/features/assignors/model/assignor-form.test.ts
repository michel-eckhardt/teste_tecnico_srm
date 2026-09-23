import { describe, expect, it } from 'vitest';

import { assignorFormSchema, toAssignorRequest } from './assignor-form';

describe('assignor form', () => {
  it('accepts a name and a formatted CNPJ and sends digits only', () => {
    const parsed = assignorFormSchema.parse({
      name: '  ACME Ltda ',
      document: '11.222.333/0001-81',
    });
    expect(toAssignorRequest(parsed)).toEqual({ name: 'ACME Ltda', document: '11222333000181' });
  });

  it('rejects blank names and invalid CNPJs', () => {
    const result = assignorFormSchema.safeParse({ name: '   ', document: '11.222.333/0001-80' });
    expect(result.success).toBe(false);
    expect(result.error?.issues.map((issue) => issue.message)).toEqual([
      'Informe a razão social.',
      'CNPJ inválido.',
    ]);
  });
});
