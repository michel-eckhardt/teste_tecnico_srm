import { beforeEach, describe, expect, it } from 'vitest';

import { acme } from '@/test/fixtures';

import { toCreateRequest } from './batch';
import { MAX_BATCH_ITEMS, useBatchStore, type NewBatchItem } from './batch-store';

function item(overrides: Partial<NewBatchItem> = {}): NewBatchItem {
  return {
    receivableType: 'DUPLICATA_MERCANTIL',
    receivableTypeLabel: 'Duplicata Mercantil',
    faceValue: '10000.00',
    faceCurrency: 'BRL',
    dueDate: '2026-12-22',
    paymentCurrency: 'BRL',
    preview: {
      termDays: 90,
      presentValue: '9285.99',
      discount: '714.01',
      netAmount: '9285.99',
      exchangeRate: null,
    },
    ...overrides,
  };
}

const store = () => useBatchStore.getState();

describe('batch store', () => {
  beforeEach(() => {
    store().clear();
    sessionStorage.clear();
  });

  it('fixes the payment currency with the first item and refuses other currencies', () => {
    expect(store().addItem(item({ paymentCurrency: 'USD' }))).toBe(true);
    expect(store().paymentCurrency).toBe('USD');

    expect(store().addItem(item({ paymentCurrency: 'BRL' }))).toBe(false);
    expect(store().items).toHaveLength(1);
  });

  it('releases the payment currency when the last item is removed', () => {
    store().addItem(item({ paymentCurrency: 'USD' }));
    const [first] = store().items;

    store().removeItem(first!.id);

    expect(store().items).toEqual([]);
    expect(store().paymentCurrency).toBeNull();
  });

  it('keeps the idempotency key while the batch is unchanged and drops it on any change', () => {
    store().addItem(item());
    store().setAssignor(acme);

    const key = store().submissionKey();
    expect(store().submissionKey()).toBe(key);

    store().addItem(item({ faceValue: '500.00' }));
    const next = store().submissionKey();
    expect(next).not.toBe(key);

    store().setAssignor(null);
    expect(store().submissionKey()).not.toBe(next);
  });

  it('persists the batch in sessionStorage (survives a reload, not the tab)', () => {
    store().addItem(item());

    const persisted = JSON.parse(sessionStorage.getItem('srm-credit-engine:batch') ?? '{}') as {
      state: { items: unknown[]; paymentCurrency: string };
    };
    expect(persisted.state.items).toHaveLength(1);
    expect(persisted.state.paymentCurrency).toBe('BRL');
  });

  it(`accepts at most ${MAX_BATCH_ITEMS} receivables`, () => {
    for (let index = 0; index < MAX_BATCH_ITEMS; index += 1) store().addItem(item());
    expect(store().addItem(item())).toBe(false);
    expect(store().items).toHaveLength(MAX_BATCH_ITEMS);
  });

  it('builds the creation request from the batch', () => {
    store().addItem(item());
    store().addItem(item({ faceValue: '2500.00', faceCurrency: 'USD', dueDate: '2026-11-10' }));
    expect(toCreateRequest(store())).toBeNull(); // no assignor yet

    store().setAssignor(acme);

    expect(toCreateRequest(store())).toEqual({
      assignorId: acme.id,
      paymentCurrency: 'BRL',
      receivables: [
        {
          receivableType: 'DUPLICATA_MERCANTIL',
          faceValue: '10000.00',
          faceCurrency: 'BRL',
          dueDate: '2026-12-22',
        },
        {
          receivableType: 'DUPLICATA_MERCANTIL',
          faceValue: '2500.00',
          faceCurrency: 'USD',
          dueDate: '2026-11-10',
        },
      ],
    });
  });
});
