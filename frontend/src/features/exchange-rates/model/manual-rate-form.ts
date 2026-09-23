import { z } from 'zod';

import {
  CURRENCY_CODES,
  type IsoDate,
  type ManualExchangeRateRequest,
} from '@/shared/api/contract';
import { isIsoDate } from '@/shared/lib/date';
import { isPositiveDecimal, parseDecimalInput } from '@/shared/lib/decimal';

/** Backend limits: @Digits(integer = 11, fraction = 8). */
const RATE_LIMITS = { maxIntegerDigits: 11, maxFractionDigits: 8 };

/**
 * Manual rate, validated like the backend: distinct currencies, a positive rate with up to 8
 * decimals (typed with comma or dot, sent as an exact decimal string) and a reference date that is
 * not in the future (optional: the server defaults to today).
 */
export function createManualRateSchema(today: IsoDate) {
  return z
    .object({
      base: z.enum(CURRENCY_CODES),
      quote: z.enum(CURRENCY_CODES),
      rate: z.string().superRefine((value, ctx) => {
        const parsed = parseDecimalInput(value, RATE_LIMITS);
        if (value.trim() === '') {
          ctx.addIssue({ code: 'custom', message: 'Informe a taxa.' });
        } else if (parsed === null) {
          ctx.addIssue({
            code: 'custom',
            message: 'Use um número com até 11 dígitos inteiros e 8 casas decimais.',
          });
        } else if (!isPositiveDecimal(parsed)) {
          ctx.addIssue({ code: 'custom', message: 'A taxa deve ser maior que zero.' });
        }
      }),
      referenceDate: z
        .string()
        .refine((value) => value === '' || isIsoDate(value), 'Data inválida.')
        .refine((value) => value === '' || value <= today, 'A data não pode estar no futuro.'),
    })
    .refine((values) => values.base !== values.quote, {
      message: 'A moeda cotada deve ser diferente da base.',
      path: ['quote'],
    });
}

export type ManualRateFormValues = z.input<ReturnType<typeof createManualRateSchema>>;

export const EMPTY_MANUAL_RATE: ManualRateFormValues = {
  base: 'USD',
  quote: 'BRL',
  rate: '',
  referenceDate: '',
};

export function toManualRateRequest(values: ManualRateFormValues): ManualExchangeRateRequest {
  const rate = parseDecimalInput(values.rate, RATE_LIMITS);
  if (rate === null) throw new RangeError(`Invalid rate: ${values.rate}`);
  return {
    base: values.base,
    quote: values.quote,
    rate,
    ...(values.referenceDate ? { referenceDate: values.referenceDate } : {}),
  };
}

/** Server field names → form fields (the cross-field rule is reported as `distinctCurrencies`). */
export function formFieldOf(serverField: string): keyof ManualRateFormValues | undefined {
  switch (serverField) {
    case 'base':
    case 'quote':
    case 'rate':
    case 'referenceDate':
      return serverField;
    case 'distinctCurrencies':
      return 'quote';
    default:
      return undefined;
  }
}
