import { useCallback } from 'react';

import type { SimulatedReceivable } from '@/features/pricing-simulator';
import { formatMoney } from '@/shared/lib/money';
import { notifySuccess, notifyWarning } from '@/shared/ui/notify';

import { MAX_BATCH_ITEMS, useBatchStore } from '../model/batch-store';

/** Bridge between the pricing simulator and the batch: what the simulator needs to know and do. */
export function useBatchComposer() {
  const lockedPaymentCurrency = useBatchStore((state) =>
    state.items.length > 0 ? state.paymentCurrency : null,
  );
  const addItem = useBatchStore((state) => state.addItem);

  const addReceivable = useCallback(
    (receivable: SimulatedReceivable) => {
      const { simulation } = receivable;
      const added = addItem({
        receivableType: receivable.receivableType,
        receivableTypeLabel: receivable.receivableTypeLabel,
        faceValue: receivable.faceValue,
        faceCurrency: receivable.faceCurrency,
        dueDate: receivable.dueDate,
        paymentCurrency: receivable.paymentCurrency,
        preview: {
          termDays: simulation.termDays,
          presentValue: simulation.presentValue,
          discount: simulation.discount,
          netAmount: simulation.netAmount,
          exchangeRate: simulation.exchangeRate,
        },
      });
      if (added) {
        notifySuccess(
          'Recebível adicionado ao lote',
          `${receivable.receivableTypeLabel} · ${formatMoney(simulation.netAmount, receivable.paymentCurrency)} líquidos`,
        );
      } else {
        notifyWarning(
          'Recebível não adicionado',
          `O lote aceita até ${MAX_BATCH_ITEMS} recebíveis, todos pagos na mesma moeda.`,
        );
      }
    },
    [addItem],
  );

  return { lockedPaymentCurrency, addReceivable };
}
