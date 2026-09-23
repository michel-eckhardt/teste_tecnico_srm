import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { renderWithProviders } from '@/test/render';

import { CashBalancesCard } from './CashBalancesCard';

describe('CashBalancesCard', () => {
  it('shows the fund balance in each currency', async () => {
    renderWithProviders(<CashBalancesCard />);

    expect(await screen.findByText('R$ 50.000.000,00')).toBeInTheDocument();
    expect(screen.getByText('US$ 10.000.000,00')).toBeInTheDocument();
    expect(screen.getByText('Real (BRL)')).toBeInTheDocument();
    expect(screen.getByText('Dólar (USD)')).toBeInTheDocument();
  });
});
