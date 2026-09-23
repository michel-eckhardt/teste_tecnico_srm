import { z } from 'zod';

import {
  CURRENCY_CODES,
  type CurrencyCode,
  type IsoDate,
  type Simulation,
  type SimulationRequest,
} from '@/shared/api/contract';
import { addDays, isIsoDate } from '@/shared/lib/date';
import { compareMoney, isMoney, MAX_FACE_VALUE } from '@/shared/lib/money';

/** Longest term accepted by the backend (`srm.pricing.max-term-days`: 5 years). */
export const MAX_TERM_DAYS = 1825;

/**
 * Validation of one receivable, mirroring the backend rules so the operator gets immediate
 * feedback and no request is sent for data the API would reject. The due date limits depend on the
 * operation date (today), hence the factory.
 */
export function createReceivableSchema(today: IsoDate) {
  const minDueDate = addDays(today, 1);
  const maxDueDate = addDays(today, MAX_TERM_DAYS);

  return z.object({
    receivableType: z.string().min(1, 'Selecione o tipo de recebível.'),
    faceValue: z.string().superRefine((value, ctx) => {
      if (value === '') {
        ctx.addIssue({ code: 'custom', message: 'Informe o valor de face.' });
      } else if (!isMoney(value)) {
        ctx.addIssue({ code: 'custom', message: 'Valor inválido.' });
      } else if (compareMoney(value, '0') <= 0) {
        ctx.addIssue({ code: 'custom', message: 'O valor deve ser maior que zero.' });
      } else if (compareMoney(value, MAX_FACE_VALUE) > 0) {
        ctx.addIssue({
          code: 'custom',
          message: 'O valor máximo por recebível é 1.000.000.000,00.',
        });
      }
    }),
    faceCurrency: z.enum(CURRENCY_CODES),
    dueDate: z.string().superRefine((value, ctx) => {
      if (value === '') {
        ctx.addIssue({ code: 'custom', message: 'Informe o vencimento.' });
      } else if (!isIsoDate(value)) {
        ctx.addIssue({ code: 'custom', message: 'Data inválida.' });
      } else if (value < minDueDate) {
        ctx.addIssue({ code: 'custom', message: 'O vencimento deve ser posterior a hoje.' });
      } else if (value > maxDueDate) {
        ctx.addIssue({ code: 'custom', message: 'O prazo máximo é de 5 anos.' });
      }
    }),
    paymentCurrency: z.enum(CURRENCY_CODES),
  });
}

export type ReceivableFormValues = z.input<ReturnType<typeof createReceivableSchema>>;

export const EMPTY_RECEIVABLE: ReceivableFormValues = {
  receivableType: '',
  faceValue: '',
  faceCurrency: 'BRL',
  dueDate: '',
  paymentCurrency: 'BRL',
};

/** Simulation request for the current form values, or `null` while they are incomplete/invalid. */
export function toSimulationRequest(values: unknown, today: IsoDate): SimulationRequest | null {
  const parsed = createReceivableSchema(today).safeParse(values);
  return parsed.success ? parsed.data : null;
}

/** A receivable whose pricing was simulated: what the batch receives. */
export interface SimulatedReceivable {
  receivableType: string;
  receivableTypeLabel: string;
  faceValue: string;
  faceCurrency: CurrencyCode;
  dueDate: IsoDate;
  paymentCurrency: CurrencyCode;
  simulation: Simulation;
}
