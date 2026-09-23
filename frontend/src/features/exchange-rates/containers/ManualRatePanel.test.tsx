import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { ManualExchangeRateRequest } from '@/shared/api/contract';
import { apiUrl } from '@/test/api';
import { usdBrlRate } from '@/test/fixtures';
import { renderWithProviders } from '@/test/render';
import { server } from '@/test/server';

import { ManualRatePanel } from './ManualRatePanel';

describe('ManualRatePanel', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'], now: new Date(2026, 8, 23, 10, 0) });
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('registers a rate typed with a comma as an exact decimal string', async () => {
    let sent: ManualExchangeRateRequest | undefined;
    server.use(
      http.post(apiUrl('/exchange-rates'), async ({ request }) => {
        sent = (await request.json()) as ManualExchangeRateRequest;
        return HttpResponse.json(
          { ...usdBrlRate, ...sent, source: 'MANUAL', id: 'manual' },
          { status: 201 },
        );
      }),
    );
    const { user } = renderWithProviders(<ManualRatePanel />);

    await user.type(screen.getByRole('textbox', { name: /^Taxa/ }), '5,20001234');
    await user.type(screen.getByRole('textbox', { name: /Data de referência/ }), '22/09/2026');
    await user.click(screen.getByRole('button', { name: 'Cadastrar taxa' }));

    await waitFor(() => {
      expect(sent).toEqual({
        base: 'USD',
        quote: 'BRL',
        rate: '5.20001234',
        referenceDate: '2026-09-22',
      });
    });
    expect(await screen.findByText('Taxa cadastrada')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: /^Taxa/ })).toHaveValue('');
  });

  it('validates before sending', async () => {
    let posted = false;
    server.use(
      http.post(apiUrl('/exchange-rates'), () => {
        posted = true;
        return HttpResponse.json({}, { status: 201 });
      }),
    );
    const { user } = renderWithProviders(<ManualRatePanel />);

    await user.click(screen.getByRole('combobox', { name: /Moeda cotada/ }));
    await user.click(await screen.findByRole('option', { name: 'Dólar (USD)' }));
    await user.type(screen.getByRole('textbox', { name: /^Taxa/ }), '0');
    await user.click(screen.getByRole('button', { name: 'Cadastrar taxa' }));

    expect(await screen.findByText('A moeda cotada deve ser diferente da base.')).toBeVisible();
    expect(screen.getByText('A taxa deve ser maior que zero.')).toBeVisible();
    expect(posted).toBe(false);
  });

  it('shows server field violations next to the fields', async () => {
    server.use(
      http.post(apiUrl('/exchange-rates'), () =>
        HttpResponse.json(
          {
            title: 'Requisição inválida',
            status: 400,
            detail: 'Um ou mais campos são inválidos.',
            code: 'VALIDATION_ERROR',
            correlationId: 'cid-400',
            errors: [
              { field: 'distinctCurrencies', message: 'base e quote devem ser moedas diferentes' },
            ],
          },
          { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );
    const { user } = renderWithProviders(<ManualRatePanel />);

    await user.type(screen.getByRole('textbox', { name: /^Taxa/ }), '5.1');
    await user.click(screen.getByRole('button', { name: 'Cadastrar taxa' }));

    expect(await screen.findByText('base e quote devem ser moedas diferentes')).toBeVisible();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
