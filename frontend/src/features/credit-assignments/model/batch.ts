import type {
  AssignorSummary,
  CreateCreditAssignmentRequest,
  CurrencyCode,
  DecimalString,
} from '@/shared/api/contract';
import { sumMoney } from '@/shared/lib/money';

import type { BatchItem } from './batch-store';

/** Face value total per face currency (a batch may mix BRL and USD receivables). */
export function faceTotalsByCurrency(
  items: readonly BatchItem[],
): { currency: CurrencyCode; total: DecimalString }[] {
  const byCurrency = new Map<CurrencyCode, DecimalString[]>();
  for (const item of items) {
    byCurrency.set(item.faceCurrency, [
      ...(byCurrency.get(item.faceCurrency) ?? []),
      item.faceValue,
    ]);
  }
  return [...byCurrency.entries()].map(([currency, values]) => ({
    currency,
    total: sumMoney(values),
  }));
}

/** Estimated net amount of the batch, in the payment currency (sum of the simulated items). */
export function estimatedNetTotal(items: readonly BatchItem[]): DecimalString {
  return sumMoney(items.map((item) => item.preview.netAmount));
}

export interface BatchDraft {
  assignor: AssignorSummary | null;
  paymentCurrency: CurrencyCode | null;
  items: readonly BatchItem[];
}

/** Why the batch cannot be submitted yet, or null when it can. */
export function batchBlocker(batch: BatchDraft): string | null {
  if (batch.items.length === 0 || batch.paymentCurrency === null) {
    return 'Adicione ao menos um recebível ao lote.';
  }
  if (batch.assignor === null) return 'Selecione o cedente.';
  return null;
}

export function toCreateRequest(batch: BatchDraft): CreateCreditAssignmentRequest | null {
  if (batchBlocker(batch) !== null || !batch.assignor || !batch.paymentCurrency) return null;
  return {
    assignorId: batch.assignor.id,
    paymentCurrency: batch.paymentCurrency,
    receivables: batch.items.map((item) => ({
      receivableType: item.receivableType,
      faceValue: item.faceValue,
      faceCurrency: item.faceCurrency,
      dueDate: item.dueDate,
    })),
  };
}
