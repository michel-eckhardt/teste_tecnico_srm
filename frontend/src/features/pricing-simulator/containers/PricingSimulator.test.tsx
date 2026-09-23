import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { SimulationRequest } from '@/shared/api/contract';
import { apiUrl } from '@/test/api';
import { problem, simulationForRequest, usdBrlRate } from '@/test/fixtures';
import { renderWithProviders } from '@/test/render';
import { server } from '@/test/server';

import { SIMULATION_DEBOUNCE_MS } from '../hooks/usePricingSimulation';
import { PricingSimulator } from './PricingSimulator';

type User = ReturnType<typeof renderWithProviders>['user'];

function captureSimulations(respond?: (body: SimulationRequest) => Response) {
  const requests: SimulationRequest[] = [];
  server.use(
    http.post(apiUrl('/pricing/simulations'), async ({ request }) => {
      const body = (await request.json()) as SimulationRequest;
      requests.push(body);
      return (
        respond?.(body) ??
        HttpResponse.json(
          simulationForRequest(body, {
            exchangeRate: {
              id: usdBrlRate.id,
              base: 'USD',
              quote: 'BRL',
              rate: usdBrlRate.rate,
              referenceDate: usdBrlRate.referenceDate,
            },
            netAmount: '1809.36',
          }),
        )
      );
    }),
  );
  return requests;
}

async function chooseType(user: User, name: RegExp) {
  const select = screen.getByRole('combobox', { name: /Tipo de recebível/ });
  await waitFor(() => {
    expect(select).toBeEnabled();
  });
  await user.click(select);
  await user.click(await screen.findByRole('option', { name }));
}

async function choosePaymentCurrency(user: User, currency: 'BRL' | 'USD') {
  const group = screen.getByRole('radiogroup', { name: 'Moeda de pagamento' });
  await user.click(within(group).getByLabelText(currency));
}

/** Resolves after the debounce window, so "no request" assertions are meaningful. */
const debounceWindow = () =>
  new Promise((resolve) => setTimeout(resolve, SIMULATION_DEBOUNCE_MS + 150));

function renderSimulator(onAddToBatch = vi.fn()) {
  const view = renderWithProviders(
    <PricingSimulator lockedPaymentCurrency={null} onAddToBatch={onAddToBatch} />,
  );
  return { ...view, onAddToBatch };
}

describe('PricingSimulator', () => {
  beforeEach(() => {
    // Only the calendar is fixed (due date rules); timers stay real for the debounce.
    vi.useFakeTimers({ toFake: ['Date'], now: new Date(2026, 8, 23, 10, 0) });
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('sends one debounced simulation for the typed receivable and shows the net amount', async () => {
    const requests = captureSimulations();
    const { user } = renderSimulator();

    await chooseType(user, /Duplicata Mercantil/);
    await choosePaymentCurrency(user, 'USD');
    await user.type(screen.getByRole('textbox', { name: /Vencimento/ }), '22/12/2026');
    await user.type(screen.getByRole('textbox', { name: /Valor de face/ }), '1000000');

    expect(screen.getByRole('textbox', { name: /Valor de face/ })).toHaveValue('10.000,00');
    expect(await screen.findByTestId('net-amount')).toHaveTextContent('US$ 1.809,36');
    await debounceWindow();
    expect(requests).toEqual([
      {
        receivableType: 'DUPLICATA_MERCANTIL',
        faceValue: '10000.00',
        faceCurrency: 'BRL',
        dueDate: '2026-12-22',
        paymentCurrency: 'USD',
      },
    ]);
    expect(screen.getByText('1 USD = 5,1322 BRL')).toBeInTheDocument();
    expect(screen.getByText('2,50%')).toBeInTheDocument();
  });

  it('does not call the API while the receivable is incomplete', async () => {
    const requests = captureSimulations();
    const { user } = renderSimulator();

    await chooseType(user, /Duplicata Mercantil/);
    await user.type(screen.getByRole('textbox', { name: /Valor de face/ }), '500000');
    await debounceWindow();

    expect(requests).toHaveLength(0);
    expect(screen.getByText('Nenhum recebível para simular')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Adicionar ao lote' })).toBeDisabled();
  });

  it('shows business rejections (422) inline with guidance', async () => {
    captureSimulations(() =>
      HttpResponse.json(
        problem(
          422,
          'EXCHANGE_RATE_STALE',
          'Taxa de câmbio desatualizada',
          'A taxa USD/BRL de 10/09/2026 é mais antiga que o limite de 5 dias.',
        ),
        { status: 422, headers: { 'Content-Type': 'application/problem+json' } },
      ),
    );
    const { user } = renderSimulator();

    await chooseType(user, /Cheque/);
    await choosePaymentCurrency(user, 'USD');
    await user.type(screen.getByRole('textbox', { name: /Vencimento/ }), '10/11/2026');
    await user.type(screen.getByRole('textbox', { name: /Valor de face/ }), '250000');

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Taxa de câmbio desatualizada');
    expect(alert).toHaveTextContent('mais antiga que o limite de 5 dias');
    expect(alert).toHaveTextContent('Sincronize ou cadastre uma taxa na tela Câmbio');
    expect(alert).toHaveTextContent('cid-exchange_rate_stale');
    expect(screen.getByRole('button', { name: 'Adicionar ao lote' })).toBeDisabled();
  });

  it('adds the simulated receivable to the batch and clears value and due date', async () => {
    captureSimulations();
    const { user, onAddToBatch } = renderSimulator();

    await chooseType(user, /Duplicata Mercantil/);
    await user.type(screen.getByRole('textbox', { name: /Vencimento/ }), '22/12/2026');
    await user.type(screen.getByRole('textbox', { name: /Valor de face/ }), '1000000');
    const addButton = screen.getByRole('button', { name: 'Adicionar ao lote' });
    await waitFor(() => {
      expect(addButton).toBeEnabled();
    });
    await user.click(addButton);

    expect(onAddToBatch).toHaveBeenCalledWith(
      expect.objectContaining({
        receivableType: 'DUPLICATA_MERCANTIL',
        receivableTypeLabel: 'Duplicata Mercantil',
        faceValue: '10000.00',
        faceCurrency: 'BRL',
        dueDate: '2026-12-22',
        paymentCurrency: 'BRL',
        simulation: expect.objectContaining({ netAmount: '1809.36' }) as unknown,
      }),
    );
    expect(screen.getByRole('textbox', { name: /Valor de face/ })).toHaveValue('');
    expect(screen.getByRole('textbox', { name: /Vencimento/ })).toHaveValue('');
  });

  it('locks the payment currency imposed by the batch', async () => {
    const requests = captureSimulations();
    const { user } = renderWithProviders(
      <PricingSimulator lockedPaymentCurrency="USD" onAddToBatch={vi.fn()} />,
    );

    const group = screen.getByRole('radiogroup', { name: 'Moeda de pagamento' });
    expect(within(group).getByLabelText('USD')).toBeChecked();
    expect(within(group).getByLabelText('BRL')).toBeDisabled();

    await chooseType(user, /Duplicata Mercantil/);
    await user.type(screen.getByRole('textbox', { name: /Vencimento/ }), '22/12/2026');
    await user.type(screen.getByRole('textbox', { name: /Valor de face/ }), '100');
    await waitFor(() => {
      expect(requests).toHaveLength(1);
    });
    expect(requests[0]?.paymentCurrency).toBe('USD');
  });
});
