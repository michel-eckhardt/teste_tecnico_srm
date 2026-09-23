import { TextInput, type TextInputProps } from '@mantine/core';

import type { CurrencyCode, DecimalString } from '@/shared/api/contract';
import { formatMoneyInput, maskMoneyInput } from '@/shared/lib/money';

const CURRENCY_SYMBOLS: Record<CurrencyCode, string> = { BRL: 'R$', USD: 'US$' };

interface MoneyInputProps extends Omit<TextInputProps, 'value' | 'onChange' | 'type'> {
  /** Canonical decimal string (`"1234.56"`), or `""` when empty. */
  value: DecimalString;
  onChange: (value: DecimalString) => void;
  currency?: CurrencyCode;
}

/**
 * Money field with a bank-style pt-BR mask (digits fill from the cents: `123456` → `1.234,56`).
 * The value in and out is always an exact decimal string, never a JS number.
 */
export function MoneyInput({ value, onChange, currency, ...props }: MoneyInputProps) {
  return (
    <TextInput
      {...props}
      inputMode="numeric"
      autoComplete="off"
      placeholder={props.placeholder ?? '0,00'}
      leftSection={currency ? CURRENCY_SYMBOLS[currency] : undefined}
      leftSectionWidth={currency ? 44 : undefined}
      styles={{ input: { fontVariantNumeric: 'tabular-nums' } }}
      value={formatMoneyInput(value)}
      onChange={(event) => {
        onChange(maskMoneyInput(event.currentTarget.value).value);
      }}
    />
  );
}
