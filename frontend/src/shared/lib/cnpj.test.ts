import { describe, expect, it } from 'vitest';

import { formatCnpj, isValidCnpj, maskCnpj, normalizeCnpj } from './cnpj';

describe('isValidCnpj', () => {
  it('accepts CNPJs with valid check digits, formatted or not', () => {
    expect(isValidCnpj('11222333000181')).toBe(true);
    expect(isValidCnpj('11.222.333/0001-81')).toBe(true);
    expect(isValidCnpj('11444777000161')).toBe(true);
  });

  it('rejects wrong check digits, wrong lengths and repeated digits (like the backend)', () => {
    expect(isValidCnpj('11222333000180')).toBe(false);
    expect(isValidCnpj('11222333000191')).toBe(false);
    expect(isValidCnpj('1122233300018')).toBe(false);
    expect(isValidCnpj('00000000000000')).toBe(false);
    expect(isValidCnpj('11111111111111')).toBe(false);
    expect(isValidCnpj('')).toBe(false);
  });
});

describe('CNPJ masks', () => {
  it('masks progressively while typing', () => {
    expect(maskCnpj('11')).toBe('11');
    expect(maskCnpj('112')).toBe('11.2');
    expect(maskCnpj('11222333')).toBe('11.222.333');
    expect(maskCnpj('112223330001')).toBe('11.222.333/0001');
    expect(maskCnpj('11222333000181999')).toBe('11.222.333/0001-81');
  });

  it('normalizes and formats stored values', () => {
    expect(normalizeCnpj('11.222.333/0001-81')).toBe('11222333000181');
    expect(formatCnpj('11222333000181')).toBe('11.222.333/0001-81');
    expect(formatCnpj('abc')).toBe('abc');
  });
});
