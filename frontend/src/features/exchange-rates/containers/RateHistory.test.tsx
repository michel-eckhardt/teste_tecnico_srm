import { screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { renderWithProviders } from '@/test/render';
import { server } from '@/test/server';

import { RateHistory } from './RateHistory';

let historyRequests: Record<string, string>[] = [];

describe('RateHistory', () => {
  beforeEach(() => {
    historyRequests = [];
    server.events.on('request:start', ({ request }) => {
      const url = new URL(request.url);
      if (url.pathname === '/api/v1/exchange-rates') {
        historyRequests.push(Object.fromEntries(url.searchParams));
      }
    });
  });
  afterEach(() => {
    server.events.removeAllListeners();
  });

  it('pages through the stored USD/BRL rates on the server', async () => {
    const { user } = renderWithProviders(<RateHistory />);

    expect(await screen.findByText('23/09/2026')).toBeInTheDocument();
    expect(screen.getByText('12 registro(s)')).toBeInTheDocument();
    expect(historyRequests.at(-1)).toEqual({ base: 'USD', quote: 'BRL', page: '0', size: '10' });

    await user.click(screen.getByRole('button', { name: 'Próxima página' }));

    expect(await screen.findByText('13/09/2026')).toBeInTheDocument();
    expect(historyRequests.at(-1)).toMatchObject({ page: '1' });
  });

  it('explains that a pair without stored rates is derived', async () => {
    const { user } = renderWithProviders(<RateHistory />);
    await screen.findByText('23/09/2026');

    await user.click(screen.getByLabelText('BRL/USD'));

    expect(await screen.findByText('Nenhuma taxa BRL/USD registrada')).toBeInTheDocument();
    await waitFor(() => {
      expect(historyRequests.at(-1)).toMatchObject({ base: 'BRL', quote: 'USD', page: '0' });
    });
  });
});
