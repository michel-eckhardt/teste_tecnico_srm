import { Text, type TextProps } from '@mantine/core';

import type { CurrencyCode, DecimalString } from '@/shared/api/contract';
import { formatMoney } from '@/shared/lib/money';

interface MoneyTextProps extends Omit<TextProps, 'children'> {
  amount: DecimalString;
  currency: CurrencyCode;
}

/** Amount formatted in pt-BR from its exact decimal string, with tabular digits for columns. */
export function MoneyText({ amount, currency, ...props }: MoneyTextProps) {
  return (
    <Text component="span" inherit style={{ fontVariantNumeric: 'tabular-nums' }} {...props}>
      {formatMoney(amount, currency)}
    </Text>
  );
}
