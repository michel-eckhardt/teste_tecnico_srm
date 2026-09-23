/**
 * CNPJ helpers mirroring the backend validation (Hibernate Validator `@CNPJ`, numeric format):
 * 14 digits, both mod-11 check digits, and no repeated-digit sequences such as `00000000000000`.
 * Punctuation is accepted on input and stripped before sending.
 */

const FIRST_WEIGHTS = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
const SECOND_WEIGHTS = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

export function normalizeCnpj(value: string): string {
  return value.replace(/\D/g, '');
}

export function isValidCnpj(value: string): boolean {
  const digits = normalizeCnpj(value);
  if (!/^\d{14}$/.test(digits) || /^(\d)\1{13}$/.test(digits)) {
    return false;
  }
  const numbers = (digits.match(/\d/g) ?? []).map(Number);
  const first = checkDigit(numbers.slice(0, 12), FIRST_WEIGHTS);
  const second = checkDigit(numbers.slice(0, 13), SECOND_WEIGHTS);
  return first === numbers[12] && second === numbers[13];
}

function checkDigit(numbers: readonly number[], weights: readonly number[]): number {
  const sum = numbers.reduce((total, digit, index) => total + digit * (weights[index] ?? 0), 0);
  const remainder = sum % 11;
  return remainder < 2 ? 0 : 11 - remainder;
}

/**
 * Progressive mask while typing: `"11222333"` → `"11.222.333"`, `"11222333000181"` →
 * `"11.222.333/0001-81"`. Extra digits are dropped.
 */
export function maskCnpj(value: string): string {
  const digits = normalizeCnpj(value).slice(0, 14);
  const parts = [
    digits.slice(0, 2),
    digits.slice(2, 5),
    digits.slice(5, 8),
    digits.slice(8, 12),
    digits.slice(12, 14),
  ];
  let masked = parts[0] ?? '';
  if (parts[1]) masked += `.${parts[1]}`;
  if (parts[2]) masked += `.${parts[2]}`;
  if (parts[3]) masked += `/${parts[3]}`;
  if (parts[4]) masked += `-${parts[4]}`;
  return masked;
}

/** Formats a stored CNPJ (14 digits) for display; anything else is returned unchanged. */
export function formatCnpj(value: string): string {
  return /^\d{14}$/.test(value) ? maskCnpj(value) : value;
}
