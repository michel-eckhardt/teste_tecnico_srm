import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it } from 'vitest';

import { apiUrl } from '@/test/api';
import { problem, usdBrlRate } from '@/test/fixtures';
import { renderWithProviders } from '@/test/render';
import { server } from '@/test/server';

import { CurrentRates } from './CurrentRates';

const card = (pair: string) => screen.findByRole('region', { name: `Taxa vigente ${pair}` });

describe('CurrentRates', () => {
  afterEach(() => {
    server.events.removeAllListeners();
  });

  it('shows both pairs with source, reference date and the derived flag', async () => {
    renderWithProviders(<CurrentRates />);

    const usdBrl = await card('USD/BRL');
    expect(await within(usdBrl).findByText('1 USD = 5,1322 BRL')).toBeInTheDocument();
    expect(within(usdBrl).getByText('Frankfurter (BCE)')).toBeInTheDocument();
    expect(within(usdBrl).getByText('Vigente')).toBeInTheDocument();
    expect(within(usdBrl).getByText(/Referência 23\/09\/2026/)).toBeInTheDocument();

    const brlUsd = await card('BRL/USD');
    expect(await within(brlUsd).findByText('1 BRL = 0,19484821 USD')).toBeInTheDocument();
    expect(within(brlUsd).getByText('Derivada de USD/BRL')).toBeInTheDocument();
  });

  it('flags a stale rate and explains the consequence', async () => {
    server.use(
      http.get(apiUrl('/exchange-rates/latest'), () =>
        HttpResponse.json({ ...usdBrlRate, referenceDate: '2026-09-10', stale: true }),
      ),
    );
    renderWithProviders(<CurrentRates />);

    const usdBrl = await card('USD/BRL');
    expect(await within(usdBrl).findByText('Desatualizada')).toBeInTheDocument();
    expect(within(usdBrl).getByText('Cotação desatualizada')).toBeInTheDocument();
    expect(within(usdBrl).getByText(/operações entre moedas serão recusadas/)).toBeInTheDocument();
  });

  it('explains when a pair has no rate at all (404)', async () => {
    server.use(
      http.get(apiUrl('/exchange-rates/latest'), () =>
        HttpResponse.json(
          problem(404, 'RESOURCE_NOT_FOUND', 'Recurso não encontrado', 'Sem taxa.'),
          { status: 404, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );
    renderWithProviders(<CurrentRates />);

    expect(
      await within(await card('USD/BRL')).findByText(/Nenhuma taxa cadastrada para USD\/BRL/),
    ).toBeInTheDocument();
  });

  it('explains a Frankfurter outage (503) without treating it as an application failure', async () => {
    server.use(
      http.post(apiUrl('/exchange-rates/sync'), () =>
        HttpResponse.json(
          problem(
            503,
            'FX_PROVIDER_UNAVAILABLE',
            'Provedor de câmbio indisponível',
            'Frankfurter indisponível.',
          ),
          { status: 503, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );
    const { user } = renderWithProviders(<CurrentRates />);
    await card('USD/BRL');

    await user.click(screen.getByRole('button', { name: 'Sincronizar com Frankfurter' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Provedor de câmbio indisponível');
    expect(alert).toHaveTextContent('As taxas já cadastradas continuam valendo');
    expect(alert).toHaveTextContent('cid-fx_provider_unavailable');
    // the rates on screen are untouched
    expect(screen.getByText('1 USD = 5,1322 BRL')).toBeInTheDocument();
  });

  it('synchronizes, reports the received rates and reloads the current ones', async () => {
    let latestRequests = 0;
    server.events.on('request:start', ({ request }) => {
      if (new URL(request.url).pathname === '/api/v1/exchange-rates/latest') latestRequests += 1;
    });
    const { user } = renderWithProviders(<CurrentRates />);
    await within(await card('USD/BRL')).findByText('1 USD = 5,1322 BRL');
    expect(latestRequests).toBe(2);

    await user.click(screen.getByRole('button', { name: 'Sincronizar com Frankfurter' }));

    expect(await screen.findByText('Taxas sincronizadas com a Frankfurter')).toBeInTheDocument();
    expect(screen.getByText('USD/BRL 5,1322')).toBeInTheDocument();
    await waitFor(() => {
      expect(latestRequests).toBe(4);
    });
  });
});
