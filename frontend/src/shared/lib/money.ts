import type { CurrencyCode, DecimalString } from '@/shared/api/contract';

/**
 * Money helpers. Amounts are exact decimal strings end to end (API contract): they are never turned
 * into JS numbers (IEEE 754), only formatted (Intl accepts numeric strings exactly, ES2023) or
 * converted to integer cents (BigInt) for client-side sums and comparisons.
 */

/** Largest face value accepted by the backend for one receivable (ReceivableLimits). */
export const MAX_FACE_VALUE: DecimalString = '1000000000.00';
/** Integer digits accepted by the backend (`@Digits(integer = 13, fraction = 2)`). */
const MAX_INTEGER_DIGITS = 13;

const MONEY_PATTERN = /^-?\d+(\.\d{1,2})?$/;

const currencyFormatters = new Map<string, Intl.NumberFormat>();

function currencyFormatter(currency: CurrencyCode): Intl.NumberFormat {
  let formatter = currencyFormatters.get(currency);
  if (!formatter) {
    formatter = new Intl.NumberFormat('pt-BR', { style: 'currency', currency });
    currencyFormatters.set(currency, formatter);
  }
  return formatter;
}

/** Numeric string accepted by `Intl.NumberFormat#format` without going through `number`. */
function asNumericLiteral(value: DecimalString): `${number}` {
  return value as `${number}`;
}

export function isMoney(value: string): boolean {
  return MONEY_PATTERN.test(value);
}

/** `"9285.99", "BRL"` → `"R$ 9.285,99"`; `"1809.36", "USD"` → `"US$ 1.809,36"`. */
export function formatMoney(amount: DecimalString, currency: CurrencyCode): string {
  return currencyFormatter(currency).format(asNumericLiteral(amount));
}

/** Integer cents of a decimal string with up to 2 fraction digits (`"10.5"` → `1050n`). */
export function toCents(amount: DecimalString): bigint {
  if (!isMoney(amount)) {
    throw new RangeError(`Invalid money amount: "${amount}"`);
  }
  const negative = amount.startsWith('-');
  const [integer = '0', fraction = ''] = (negative ? amount.slice(1) : amount).split('.');
  const cents = BigInt(integer) * 100n + BigInt(fraction.padEnd(2, '0'));
  return negative ? -cents : cents;
}

/** `1050n` → `"10.50"`. */
export function fromCents(cents: bigint): DecimalString {
  const negative = cents < 0n;
  const absolute = negative ? -cents : cents;
  const integer = (absolute / 100n).toString();
  const fraction = (absolute % 100n).toString().padStart(2, '0');
  return `${negative ? '-' : ''}${integer}.${fraction}`;
}

/** Exact sum of money strings. */
export function sumMoney(amounts: readonly DecimalString[]): DecimalString {
  return fromCents(amounts.reduce((total, amount) => total + toCents(amount), 0n));
}

export function compareMoney(a: DecimalString, b: DecimalString): -1 | 0 | 1 {
  const difference = toCents(a) - toCents(b);
  return difference === 0n ? 0 : difference < 0n ? -1 : 1;
}

/**
 * Bank-style money mask for text inputs: typed digits fill the amount from the cents
 * (`"1"` → `0,01`, `"123456"` → `1.234,56`), so the operator never types a decimal separator and a
 * pasted `"1.234,56"` keeps its meaning. Returns the text to display and the canonical decimal
 * string for the API (`""` when nothing was typed).
 */
export function maskMoneyInput(raw: string): { display: string; value: DecimalString } {
  const digits = raw
    .replace(/\D/g, '')
    .replace(/^0+/, '')
    .slice(0, MAX_INTEGER_DIGITS + 2);
  if (digits === '') {
    return { display: '', value: '' };
  }
  const padded = digits.padStart(3, '0');
  const integer = padded.slice(0, -2);
  const fraction = padded.slice(-2);
  return { display: `${groupThousands(integer)},${fraction}`, value: `${integer}.${fraction}` };
}

/** Canonical decimal string → masked input text (`"1234.5"` → `"1.234,50"`). */
export function formatMoneyInput(value: DecimalString): string {
  if (!isMoney(value) || value.startsWith('-')) {
    return '';
  }
  return maskMoneyInput(fromCents(toCents(value)).replace('.', '')).display;
}

function groupThousands(integer: string): string {
  return integer.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}
