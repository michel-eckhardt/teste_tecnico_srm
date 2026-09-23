import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { CreateCreditAssignmentRequest } from '@/shared/api/contract';
import { apiUrl } from '@/test/api';
import { acme, creditAssignmentFor, problem } from '@/test/fixtures';
import { renderWithProviders } from '@/test/render';
import { server } from '@/test/server';

import { useBatchStore, type NewBatchItem } from '../model/batch-store';
import { BatchBuilder } from './BatchBuilder';

const brlItem: NewBatchItem = {
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
};
const usdItem: NewBatchItem = {
  ...brlItem,
  receivableType: 'CHEQUE_PRE_DATADO',
  receivableTypeLabel: 'Cheque Pré-datado',
  faceValue: '2500.00',
  faceCurrency: 'USD',
  dueDate: '2026-11-10',
  preview: { ...brlItem.preview, netAmount: '12210.47', discount: '120.81' },
};

interface CapturedPost {
  idempotencyKey: string | null;
  body: CreateCreditAssignmentRequest;
}

function capturePosts(responses: (() => Response)[]) {
  const posts: CapturedPost[] = [];
  server.use(
    http.post(apiUrl('/credit-assignments'), async ({ request }) => {
      posts.push({
        idempotencyKey: request.headers.get('Idempotency-Key'),
        body: (await request.json()) as CreateCreditAssignmentRequest,
      });
      const respond = responses[posts.length - 1] ?? responses.at(-1);
      return respond ? respond() : HttpResponse.error();
    }),
  );
  return posts;
}

describe('BatchBuilder', () => {
  beforeEach(() => {
    useBatchStore.getState().clear();
    sessionStorage.clear();
  });

  it('shows the batch with totals per face currency and the estimated net amount', async () => {
    useBatchStore.getState().addItem(brlItem);
    useBatchStore.getState().addItem(usdItem);

    renderWithProviders(<BatchBuilder onCreated={vi.fn()} />);

    const table = await screen.findByRole('table');
    expect(within(table).getAllByRole('row')).toHaveLength(3);
    expect(screen.getByText('Pagamento em BRL')).toBeInTheDocument();
    expect(screen.getByText('R$ 21.496,46')).toBeInTheDocument(); // 9285.99 + 12210.47
    expect(screen.getAllByText('US$ 2.500,00')).toHaveLength(2); // row + face total
    expect(screen.getByText('Selecione o cedente.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Registrar operação' })).toBeDisabled();
  });

  it('submits with an Idempotency-Key and reuses it when retrying after a network failure', async () => {
    const created = creditAssignmentFor();
    const posts = capturePosts([
      () => HttpResponse.error(),
      () => HttpResponse.json(created, { status: 201, headers: { ETag: '"0"' } }),
    ]);
    useBatchStore.getState().addItem(brlItem);
    useBatchStore.getState().addItem(usdItem);
    useBatchStore.getState().setAssignor(acme);
    const onCreated = vi.fn();
    const { user } = renderWithProviders(<BatchBuilder onCreated={onCreated} />);

    await user.click(screen.getByRole('button', { name: 'Registrar operação' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Não foi possível confirmar o registro');
    expect(alert).toHaveTextContent('mesma chave de idempotência');
    expect(onCreated).not.toHaveBeenCalled();

    await user.click(screen.getByRole('button', { name: 'Registrar operação' }));

    await waitFor(() => {
      expect(onCreated).toHaveBeenCalledWith(created.id);
    });
    expect(posts).toHaveLength(2);
    expect(posts[0]?.idempotencyKey).toMatch(/^[A-Za-z0-9._:-]{1,100}$/);
    expect(posts[1]?.idempotencyKey).toBe(posts[0]?.idempotencyKey);
    expect(posts[1]?.body).toEqual({
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
          receivableType: 'CHEQUE_PRE_DATADO',
          faceValue: '2500.00',
          faceCurrency: 'USD',
          dueDate: '2026-11-10',
        },
      ],
    });
    // the batch is emptied once the operation exists
    expect(useBatchStore.getState().items).toEqual([]);
    expect(await screen.findByText('Nenhum recebível no lote')).toBeInTheDocument();
  });

  it('keeps the batch and explains business rejections', async () => {
    capturePosts([
      () =>
        HttpResponse.json(
          {
            ...problem(
              400,
              'VALIDATION_ERROR',
              'Requisição inválida',
              'Um ou mais campos são inválidos.',
            ),
            errors: [{ field: 'receivables[1].faceValue', message: 'deve ser maior que 0' }],
          },
          { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
        ),
    ]);
    useBatchStore.getState().addItem(brlItem);
    useBatchStore.getState().addItem(usdItem);
    useBatchStore.getState().setAssignor(acme);
    const { user } = renderWithProviders(<BatchBuilder onCreated={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: 'Registrar operação' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Recebível 2 · faceValue: deve ser maior que 0');
    expect(alert).toHaveTextContent('cid-validation_error');
    expect(useBatchStore.getState().items).toHaveLength(2);
  });

  it('removes items from the batch', async () => {
    useBatchStore.getState().addItem(brlItem);
    useBatchStore.getState().addItem(usdItem);
    const { user } = renderWithProviders(<BatchBuilder onCreated={vi.fn()} />);

    await user.click(await screen.findByRole('button', { name: 'Remover recebível 1 do lote' }));

    expect(useBatchStore.getState().items).toHaveLength(1);
    expect(screen.getAllByRole('row')).toHaveLength(2);
  });
});
