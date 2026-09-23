import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

import type {
  AssignorSummary,
  CurrencyCode,
  DecimalString,
  ExchangeRateSnapshot,
  IsoDate,
} from '@/shared/api/contract';
import { newIdempotencyKey } from '@/shared/lib/idempotency';

/** Receivables per operation accepted by the backend. */
export const MAX_BATCH_ITEMS = 500;

export interface BatchItem {
  /** Client-side id (list key / removal). */
  id: string;
  receivableType: string;
  receivableTypeLabel: string;
  faceValue: DecimalString;
  faceCurrency: CurrencyCode;
  dueDate: IsoDate;
  /** Pricing simulated when the item was added: informative, the server prices again. */
  preview: {
    termDays: number;
    presentValue: DecimalString;
    discount: DecimalString;
    netAmount: DecimalString;
    exchangeRate: ExchangeRateSnapshot | null;
  };
}

export type NewBatchItem = Omit<BatchItem, 'id'> & { paymentCurrency: CurrencyCode };

interface BatchData {
  assignor: AssignorSummary | null;
  /** Payment currency of the whole operation: fixed by the first item. */
  paymentCurrency: CurrencyCode | null;
  items: BatchItem[];
  /**
   * Idempotency key of the pending submission. Kept while the batch is unchanged, so resubmitting
   * after a network failure cannot create the operation twice; any change to the batch drops it.
   */
  idempotencyKey: string | null;
}

interface BatchActions {
  /** Returns false (and changes nothing) when the item does not fit the batch. */
  addItem: (item: NewBatchItem) => boolean;
  removeItem: (id: string) => void;
  setAssignor: (assignor: AssignorSummary | null) => void;
  /** Idempotency key for submitting the current batch (created on first use). */
  submissionKey: () => string;
  /** Forgets the key (e.g. the server refused it for a different payload). */
  resetSubmissionKey: () => void;
  clear: () => void;
}

export type BatchState = BatchData & BatchActions;

const EMPTY_BATCH: BatchData = {
  assignor: null,
  paymentCurrency: null,
  items: [],
  idempotencyKey: null,
};

/**
 * The batch ("lote") being assembled in the operator panel: global client state (the simulator
 * adds to it, the batch builder submits it), kept in sessionStorage so a page reload does not lose
 * the operator's work while never outliving the browser tab.
 */
export const useBatchStore = create<BatchState>()(
  persist(
    (set, get) => ({
      ...EMPTY_BATCH,

      addItem: (item) => {
        const { items, paymentCurrency } = get();
        if (items.length >= MAX_BATCH_ITEMS) return false;
        if (paymentCurrency !== null && paymentCurrency !== item.paymentCurrency) return false;
        const { paymentCurrency: itemCurrency, ...data } = item;
        set({
          paymentCurrency: itemCurrency,
          items: [...items, { ...data, id: crypto.randomUUID() }],
          idempotencyKey: null,
        });
        return true;
      },

      removeItem: (id) => {
        const items = get().items.filter((item) => item.id !== id);
        set({
          items,
          paymentCurrency: items.length === 0 ? null : get().paymentCurrency,
          idempotencyKey: null,
        });
      },

      setAssignor: (assignor) => {
        set({ assignor, idempotencyKey: null });
      },

      submissionKey: () => {
        const current = get().idempotencyKey;
        if (current) return current;
        const key = newIdempotencyKey();
        set({ idempotencyKey: key });
        return key;
      },

      resetSubmissionKey: () => {
        set({ idempotencyKey: null });
      },

      clear: () => {
        set(EMPTY_BATCH);
      },
    }),
    {
      name: 'srm-credit-engine:batch',
      version: 1,
      storage: createJSONStorage(() => sessionStorage),
      partialize: ({ assignor, paymentCurrency, items, idempotencyKey }) => ({
        assignor,
        paymentCurrency,
        items,
        idempotencyKey,
      }),
    },
  ),
);
