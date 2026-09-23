import { describe, expect, it } from 'vitest';

import {
  addDecimals,
  formatDecimal,
  formatPercent,
  formatRate,
  isPositiveDecimal,
  parseDecimalInput,
} from './decimal';

describe('decimal formatting', () => {
  it('formats monthly rates as percentages', () => {
    expect(formatPercent('0.02500000')).toBe('2,50%');
    expect(formatPercent('0.01000000')).toBe('1,00%');
    expect(formatPercent('0.01234567')).toBe('1,2346%');
  });

  it('formats exchange rates with 4 to 8 decimals', () => {
    expect(formatRate('5.13220000')).toBe('5,1322');
    expect(formatRate('0.19484821')).toBe('0,19484821');
  });

  it('formats plain decimals without trailing zeros', () => {
    expect(formatDecimal('3.00000000')).toBe('3');
    expect(formatDecimal('2.96666667')).toBe('2,96666667');
  });
});

describe('parseDecimalInput', () => {
  const limits = { maxIntegerDigits: 11, maxFractionDigits: 8 };

  it('accepts comma or dot as the decimal separator', () => {
    expect(parseDecimalInput('5,1322', limits)).toBe('5.1322');
    expect(parseDecimalInput(' 5.2 ', limits)).toBe('5.2');
    expect(parseDecimalInput('005', limits)).toBe('5');
    expect(parseDecimalInput('0.19484821', limits)).toBe('0.19484821');
  });

  it('rejects text, negatives, thousands separators and excess digits', () => {
    expect(parseDecimalInput('', limits)).toBeNull();
    expect(parseDecimalInput('abc', limits)).toBeNull();
    expect(parseDecimalInput('-5', limits)).toBeNull();
    expect(parseDecimalInput('1.234,56', limits)).toBeNull();
    expect(parseDecimalInput('1.123456789', limits)).toBeNull();
    expect(parseDecimalInput('123456789012', limits)).toBeNull();
  });

  it('detects positive values', () => {
    expect(isPositiveDecimal('0.00000001')).toBe(true);
    expect(isPositiveDecimal('0')).toBe(false);
    expect(isPositiveDecimal('0.000')).toBe(false);
  });
});

describe('addDecimals', () => {
  it('adds rates exactly, keeping the larger scale', () => {
    expect(addDecimals('0.01000000', '0.01500000')).toBe('0.02500000');
    expect(addDecimals('0.1', '0.2')).toBe('0.3');
    expect(addDecimals('0.005', '0.0250')).toBe('0.0300');
    expect(addDecimals('12', '3')).toBe('15');
    expect(addDecimals('9.99999999', '0.00000001')).toBe('10.00000000');
  });
});
