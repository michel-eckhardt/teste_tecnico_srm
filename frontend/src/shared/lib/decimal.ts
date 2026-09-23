import type { DecimalString } from '@/shared/api/contract';

/**
 * Rate / percentage helpers on exact decimal strings (never converted to JS numbers).
 */

const percentFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'percent',
  minimumFractionDigits: 2,
  maximumFractionDigits: 4,
});

const rateFormatter = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 4,
  maximumFractionDigits: 8,
});

const decimalFormatter = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 8 });

/** Monthly rate as a percentage: `"0.02500000"` → `"2,50%"`. */
export function formatPercent(rate: DecimalString): string {
  return percentFormatter.format(rate as `${number}`);
}

/** Exchange rate with 4 to 8 decimals: `"5.13220000"` → `"5,1322"`. */
export function formatRate(rate: DecimalString): string {
  return rateFormatter.format(rate as `${number}`);
}

/** Plain decimal, trailing zeros removed: `"3.00000000"` → `"3"`, `"2.96666667"` → `"2,96666667"`. */
export function formatDecimal(value: DecimalString): string {
  return decimalFormatter.format(value as `${number}`);
}

/**
 * Parses a user-typed positive decimal (comma or dot as separator, no thousands separators) into a
 * canonical string. Returns `null` when the text is not a decimal within the given limits.
 */
export function parseDecimalInput(
  raw: string,
  { maxIntegerDigits, maxFractionDigits }: { maxIntegerDigits: number; maxFractionDigits: number },
): DecimalString | null {
  const text = raw.trim().replace(',', '.');
  const match = /^(\d+)(?:\.(\d*))?$/.exec(text);
  if (!match) return null;
  const integer = (match[1] ?? '').replace(/^0+(?=\d)/, '');
  const fraction = match[2] ?? '';
  if (integer.length > maxIntegerDigits || fraction.length > maxFractionDigits) return null;
  return fraction === '' ? integer : `${integer}.${fraction}`;
}

/** True when a canonical decimal string is strictly greater than zero. */
export function isPositiveDecimal(value: DecimalString): boolean {
  return /[1-9]/.test(value) && !value.startsWith('-');
}
