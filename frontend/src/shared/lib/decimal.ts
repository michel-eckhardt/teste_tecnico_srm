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

/** Exact sum of two non-negative decimal strings, keeping the larger scale (`"0.010"` + `"0.0150"` → `"0.0250"`). */
export function addDecimals(a: DecimalString, b: DecimalString): DecimalString {
  const scale = Math.max(fractionDigits(a), fractionDigits(b));
  const units = toScaledUnits(a, scale) + toScaledUnits(b, scale);
  if (scale === 0) return units.toString();
  const text = units.toString().padStart(scale + 1, '0');
  return `${text.slice(0, -scale)}.${text.slice(-scale)}`;
}

function fractionDigits(value: DecimalString): number {
  return value.split('.')[1]?.length ?? 0;
}

function toScaledUnits(value: DecimalString, scale: number): bigint {
  const [integer = '0', fraction = ''] = value.split('.');
  return BigInt(integer) * 10n ** BigInt(scale) + BigInt(fraction.padEnd(scale, '0') || '0');
}
