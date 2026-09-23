import { describe, expect, it } from 'vitest';

import { daysBetween, formatDate, formatDateTime, isIsoDate, parseBrDate, tomorrow } from './date';

describe('calendar dates', () => {
  it('formats ISO dates as DD/MM/YYYY without time zone shifts', () => {
    expect(formatDate('2026-12-22')).toBe('22/12/2026');
    expect(formatDate('2026-01-01')).toBe('01/01/2026');
  });

  it('parses DD/MM/YYYY strictly', () => {
    expect(parseBrDate('23/09/2026')).toBe('2026-09-23');
    expect(parseBrDate('29/02/2028')).toBe('2028-02-29');
    expect(parseBrDate('29/02/2027')).toBeNull();
    expect(parseBrDate('31/04/2026')).toBeNull();
    expect(parseBrDate('2026-09-23')).toBeNull();
  });

  it('validates ISO dates', () => {
    expect(isIsoDate('2026-09-23')).toBe(true);
    expect(isIsoDate('2026-13-01')).toBe(false);
    expect(isIsoDate('23/09/2026')).toBe(false);
  });

  it('computes tomorrow and day differences', () => {
    expect(tomorrow(new Date(2026, 11, 31, 23, 59))).toBe('2027-01-01');
    expect(daysBetween('2026-09-23', '2026-12-22')).toBe(90);
    expect(daysBetween('2026-10-01', '2026-11-01')).toBe(31);
  });
});

describe('formatDateTime', () => {
  it('formats instants in the local time zone', () => {
    const instant = '2026-09-23T14:05:00Z';
    const expected = new Intl.DateTimeFormat('pt-BR', {
      dateStyle: 'short',
      timeStyle: 'short',
    }).format(new Date(instant));
    expect(formatDateTime(instant)).toBe(expected);
    expect(formatDateTime('invalid')).toBe('invalid');
  });
});
