import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { apiUrl } from '@/test/api';
import { renderWithProviders } from '@/test/render';
import { server } from '@/test/server';

import { TransactionsExplorer } from './TransactionsExplorer';

let statementRequests: URLSearchParams[] = [];

function lastRequest(): Record<string, string> {
  return Object.fromEntries(statementRequests.at(-1) ?? []);
}

function renderExplorer(route = '/transacoes') {
  return renderWithProviders(<TransactionsExplorer />, { route, path: '/transacoes' });
}

describe('TransactionsExplorer', () => {
  beforeEach(() => {
    statementRequests = [];
    server.events.on('request:start', ({ request }) => {
      const url = new URL(request.url);
      if (url.pathname === '/api/v1/reports/settlement-statement') {
        statementRequests.push(url.searchParams);
      }
    });
  });
  afterEach(() => {
    server.events.removeAllListeners();
  });

  it('loads the first page sorted by operation date and shows rows and totals', async () => {
    renderExplorer();

    expect(await screen.findByText('Cedente 1 Ltda')).toBeInTheDocument();
    expect(lastRequest()).toEqual({ page: '0', size: '20', sort: 'createdAt,desc' });
    expect(screen.getByText('Mostrando 1–20 de 1.234 operações')).toBeInTheDocument();
    const table = screen.getByRole('table');
    expect(within(table).getAllByRole('row')).toHaveLength(21);
    expect(within(table).getByRole('columnheader', { name: /Data da operação/ })).toHaveAttribute(
      'aria-sort',
      'descending',
    );
    expect(within(table).getAllByText('R$ 21.377,30')).toHaveLength(20);
  });

  it('paginates and sorts on the server', async () => {
    const { user, router } = renderExplorer();
    await screen.findByText('Cedente 1 Ltda');

    await user.click(screen.getByRole('button', { name: 'Próxima página' }));
    await waitFor(() => {
      expect(lastRequest()).toMatchObject({ page: '1' });
    });
    expect(router.state.location.search).toBe('?page=1');
    expect(await screen.findByText('Cedente 21 Ltda')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Valor líquido/ }));
    await waitFor(() => {
      expect(lastRequest()).toMatchObject({ page: '0', sort: 'totalNetAmount,desc' });
    });
    expect(new URLSearchParams(router.state.location.search).get('sort')).toBe(
      'totalNetAmount,desc',
    );

    await user.click(screen.getByRole('button', { name: /Valor líquido/ }));
    await waitFor(() => {
      expect(lastRequest()).toMatchObject({ sort: 'totalNetAmount,asc' });
    });

    // only whitelisted columns are sortable
    expect(screen.queryByRole('button', { name: /Deságio/ })).not.toBeInTheDocument();
  });

  it('changes the page size', async () => {
    const { user, router } = renderExplorer('/transacoes?page=4');
    await screen.findByText('Cedente 81 Ltda');

    await user.click(screen.getByRole('combobox', { name: 'Operações por página' }));
    await user.click(await screen.findByRole('option', { name: '50' }));

    await waitFor(() => {
      expect(lastRequest()).toMatchObject({ page: '0', size: '50' });
    });
    expect(router.state.location.search).toBe('?size=50');
    expect(await screen.findByText('Mostrando 1–50 de 1.234 operações')).toBeInTheDocument();
  });

  it('opens the operation in a drawer with its actions', async () => {
    const { user, router } = renderExplorer();
    await screen.findByText('Cedente 1 Ltda');

    await user.click(
      screen.getByRole('button', { name: 'Ver detalhes da operação de Cedente 1 Ltda' }),
    );

    const drawer = await screen.findByRole('dialog', { name: 'Detalhes da operação' });
    expect(await within(drawer).findByText('ACME Indústria Ltda')).toBeInTheDocument();
    expect(within(drawer).getByRole('button', { name: 'Liquidar' })).toBeInTheDocument();
    expect(new URLSearchParams(router.state.location.search).get('operacao')).toBe(
      '0192a000-0000-7000-8000-000000000001',
    );
  });

  it('shows an empty state that offers to clear the filters', async () => {
    server.use(
      http.get(apiUrl('/reports/settlement-statement'), () =>
        HttpResponse.json({
          content: [],
          page: { number: 0, size: 20, totalElements: 0, totalPages: 0 },
        }),
      ),
    );
    const { user, router } = renderExplorer('/transacoes?currency=USD');

    expect(await screen.findByText('Nenhuma operação encontrada')).toBeInTheDocument();
    const clearButtons = screen.getAllByRole('button', { name: 'Limpar filtros' });
    await user.click(clearButtons.at(-1)!);

    await waitFor(() => {
      expect(router.state.location.search).toBe('');
    });
  });

  it('shows errors with a retry', async () => {
    server.use(
      http.get(apiUrl('/reports/settlement-statement'), () =>
        HttpResponse.json(
          {
            title: 'Requisição inválida',
            status: 400,
            detail: 'Um ou mais parâmetros são inválidos.',
            code: 'VALIDATION_ERROR',
            correlationId: 'cid-statement',
          },
          { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );
    renderExplorer();

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Não foi possível carregar o extrato');
    expect(alert).toHaveTextContent('cid-statement');
    expect(within(alert).getByRole('button', { name: 'Tentar novamente' })).toBeInTheDocument();
  });
});
