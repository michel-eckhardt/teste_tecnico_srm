import { describe, expect, it } from 'vitest';

import {
  compareMoney,
  formatMoney,
  formatMoneyInput,
  fromCents,
  maskMoneyInput,
  sumMoney,
  toCents,
} from './money';

/** Intl uses a non-breaking space between the symbol and the amount. */
const plain = (text: string) => text.replace(/\s/g, ' ');

describe('formatMoney', () => {
  it('formats decimal strings in pt-BR for each currency', () => {
    expect(plain(formatMoney('9285.99', 'BRL'))).toBe('R$ 9.285,99');
    expect(plain(formatMoney('1809.36', 'USD'))).toBe('US$ 1.809,36');
    expect(plain(formatMoney('-714.01', 'BRL'))).toBe('-R$ 714,01');
  });

  it('keeps every digit of amounts beyond the float precision', () => {
    // As a JS number this would print 12.345.678.901.234.568,00.
    expect(plain(formatMoney('12345678901234567.89', 'BRL'))).toBe('R$ 12.345.678.901.234.567,89');
  });
});

describe('cents arithmetic', () => {
  it('converts to and from integer cents', () => {
    expect(toCents('10.5')).toBe(1050n);
    expect(toCents('0.01')).toBe(1n);
    expect(toCents('-3')).toBe(-300n);
    expect(fromCents(1050n)).toBe('10.50');
    expect(fromCents(-5n)).toBe('-0.05');
  });

  it('sums without floating point drift', () => {
    // 0.1 + 0.2 === 0.30000000000000004 with numbers
    expect(sumMoney(['0.10', '0.20'])).toBe('0.30');
    expect(sumMoney(['9999999999999.99', '0.01'])).toBe('10000000000000.00');
    expect(sumMoney([])).toBe('0.00');
  });

  it('compares amounts', () => {
    expect(compareMoney('1000000000.00', '1000000000.01')).toBe(-1);
    expect(compareMoney('10', '10.00')).toBe(0);
    expect(compareMoney('2', '1.99')).toBe(1);
  });

  it('rejects malformed amounts', () => {
    expect(() => toCents('1,50')).toThrow(RangeError);
    expect(() => toCents('1.234')).toThrow(RangeError);
  });
});

describe('maskMoneyInput', () => {
  it('fills the amount from the cents as the operator types', () => {
    expect(maskMoneyInput('1')).toEqual({ display: '0,01', value: '0.01' });
    expect(maskMoneyInput('123456')).toEqual({ display: '1.234,56', value: '1234.56' });
    expect(maskMoneyInput('1000000')).toEqual({ display: '10.000,00', value: '10000.00' });
  });

  it('keeps the meaning of pasted pt-BR amounts and ignores other characters', () => {
    expect(maskMoneyInput('R$ 1.234,56')).toEqual({ display: '1.234,56', value: '1234.56' });
    expect(maskMoneyInput('00012')).toEqual({ display: '0,12', value: '0.12' });
  });

  it('is empty when there are no digits', () => {
    expect(maskMoneyInput('')).toEqual({ display: '', value: '' });
    expect(maskMoneyInput('abc')).toEqual({ display: '', value: '' });
  });

  it('limits the amount to the digits accepted by the backend (13 + 2)', () => {
    expect(maskMoneyInput('12345678901234567').value).toBe('1234567890123.45');
  });
});

describe('formatMoneyInput', () => {
  it('renders a canonical amount as input text', () => {
    expect(formatMoneyInput('1234.5')).toBe('1.234,50');
    expect(formatMoneyInput('10000.00')).toBe('10.000,00');
    expect(formatMoneyInput('')).toBe('');
    expect(formatMoneyInput('abc')).toBe('');
  });
});
