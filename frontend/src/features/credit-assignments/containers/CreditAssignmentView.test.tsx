import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import type { CreditAssignment } from '@/shared/api/contract';
import { apiUrl } from '@/test/api';
import { creditAssignmentFor, problem } from '@/test/fixtures';
import { renderWithProviders } from '@/test/render';
import { server } from '@/test/server';

import { CreditAssignmentView } from './CreditAssignmentView';

const pending = creditAssignmentFor({ version: 3 });
const settled = creditAssignmentFor({
  status: 'SETTLED',
  version: 4,
  settledAt: '2026-09-23T14:06:10Z',
});

/** Serves the given versions of the operation, one per GET (the last one repeats). */
function serveVersions(...versions: CreditAssignment[]) {
  let gets = 0;
  server.use(
    http.get(apiUrl(`/credit-assignments/${pending.id}`), () => {
      const operation = versions[Math.min(gets, versions.length - 1)] ?? pending;
      gets += 1;
      return HttpResponse.json(operation, { headers: { ETag: `"${operation.version}"` } });
    }),
  );
  return { gets: () => gets };
}

function captureSettlements(respond: () => Response) {
  const ifMatch: (string | null)[] = [];
  server.use(
    http.post(apiUrl(`/credit-assignments/${pending.id}/settlement`), ({ request }) => {
      ifMatch.push(request.headers.get('If-Match'));
      return respond();
    }),
  );
  return ifMatch;
}

type User = ReturnType<typeof renderWithProviders>['user'];

async function settleAndConfirm(user: User) {
  await user.click(await screen.findByRole('button', { name: 'Liquidar' }));
  const dialog = await screen.findByRole('dialog', { name: 'Liquidar operação?' });
  expect(dialog).toHaveTextContent('Serão debitados R$ 21.377,30 da conta-caixa do fundo em BRL');
  await user.click(within(dialog).getByRole('button', { name: 'Liquidar' }));
}

const problemResponse = (status: number, code: string, title: string, detail: string) =>
  HttpResponse.json(problem(status, code, title, detail), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  });

describe('CreditAssignmentView', () => {
  it('shows the operation with its priced receivables', async () => {
    serveVersions(pending);
    renderWithProviders(<CreditAssignmentView operationId={pending.id} />);

    expect(await screen.findByText('ACME Indústria Ltda')).toBeInTheDocument();
    expect(screen.getByText('Pendente')).toBeInTheDocument();
    expect(screen.getByText('R$ 21.377,30')).toBeInTheDocument();
    expect(screen.getByText('1 USD = 5,1322 BRL')).toBeInTheDocument();
    expect(screen.getAllByRole('row')).toHaveLength(3);
  });

  it('settles with If-Match set to the ETag of the version on screen', async () => {
    serveVersions(pending);
    const ifMatch = captureSettlements(() =>
      HttpResponse.json(settled, { headers: { ETag: '"4"' } }),
    );
    const { user } = renderWithProviders(<CreditAssignmentView operationId={pending.id} />);

    await settleAndConfirm(user);

    expect(await screen.findByText('Liquidada')).toBeInTheDocument();
    expect(ifMatch).toEqual(['"3"']);
    expect(screen.queryByRole('button', { name: 'Liquidar' })).not.toBeInTheDocument();
    expect(await screen.findByText('Operação liquidada')).toBeInTheDocument();
  });

  it('explains a 409 (already settled) and shows the operation as it is now', async () => {
    const versions = serveVersions(pending, settled);
    captureSettlements(() =>
      problemResponse(409, 'OPERATION_ALREADY_SETTLED', 'Operação já liquidada', 'Já liquidada.'),
    );
    const { user } = renderWithProviders(<CreditAssignmentView operationId={pending.id} />);

    await settleAndConfirm(user);

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Operação já liquidada');
    expect(alert).toHaveTextContent('nenhum novo débito foi realizado');
    expect(alert).toHaveTextContent('cid-operation_already_settled');
    expect(await screen.findByText('Liquidada')).toBeInTheDocument();
    expect(versions.gets()).toBe(2);
  });

  it('reloads after a 412: someone else changed the operation meanwhile', async () => {
    const cancelledElsewhere = creditAssignmentFor({
      status: 'CANCELLED',
      version: 4,
      cancelledAt: '2026-09-23T14:06:00Z',
    });
    const versions = serveVersions(pending, cancelledElsewhere);
    const ifMatch = captureSettlements(() =>
      problemResponse(412, 'PRECONDITION_FAILED', 'Versão desatualizada', 'Versão 3 != 4.'),
    );
    const { user } = renderWithProviders(<CreditAssignmentView operationId={pending.id} />);

    await settleAndConfirm(user);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Operação alterada por outra pessoa',
    );
    expect(await screen.findByText('Cancelada')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Liquidar' })).not.toBeInTheDocument();
    expect(ifMatch).toEqual(['"3"']);
    expect(versions.gets()).toBe(2);
  });

  it('cancels after confirmation', async () => {
    serveVersions(pending);
    let cancelIfMatch: string | null = null;
    server.use(
      http.post(apiUrl(`/credit-assignments/${pending.id}/cancellation`), ({ request }) => {
        cancelIfMatch = request.headers.get('If-Match');
        return HttpResponse.json(
          creditAssignmentFor({
            status: 'CANCELLED',
            version: 4,
            cancelledAt: '2026-09-23T15:00:00Z',
          }),
          { headers: { ETag: '"4"' } },
        );
      }),
    );
    const { user } = renderWithProviders(<CreditAssignmentView operationId={pending.id} />);

    await user.click(await screen.findByRole('button', { name: 'Cancelar' }));
    const dialog = await screen.findByRole('dialog', { name: 'Cancelar operação?' });
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar operação' }));

    expect(await screen.findByText('Cancelada')).toBeInTheDocument();
    expect(cancelIfMatch).toBe('"3"');
  });
});
