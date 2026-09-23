import { screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '@/test/render';

import { OperatorPanelPage } from './OperatorPanelPage';

describe('OperatorPanelPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'], now: new Date(2026, 8, 23, 10, 0) });
    sessionStorage.clear();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('simulates a receivable, builds the batch and registers the operation', async () => {
    const { user, router } = renderWithProviders(<OperatorPanelPage />);

    // simulate and add to the batch
    const type = screen.getByRole('combobox', { name: /Tipo de recebível/ });
    await waitFor(() => {
      expect(type).toBeEnabled();
    });
    await user.click(type);
    await user.click(await screen.findByRole('option', { name: /Duplicata Mercantil/ }));
    await user.type(screen.getByRole('textbox', { name: /Vencimento/ }), '22/12/2026');
    await user.type(screen.getByRole('textbox', { name: /Valor de face/ }), '1000000');
    const add = screen.getByRole('button', { name: 'Adicionar ao lote' });
    await waitFor(() => {
      expect(add).toBeEnabled();
    });
    await user.click(add);

    // the batch now fixes the payment currency
    expect(await screen.findByText('Pagamento em BRL')).toBeInTheDocument();
    const paymentCurrency = screen.getByRole('radiogroup', { name: 'Moeda de pagamento' });
    expect(within(paymentCurrency).getByLabelText('USD')).toBeDisabled();

    // choose the assignor and register
    await user.type(screen.getByRole('combobox', { name: 'Cedente' }), 'acme');
    await user.click(await screen.findByRole('option', { name: /ACME Indústria Ltda/ }));
    await user.click(screen.getByRole('button', { name: 'Registrar operação' }));

    const created = await screen.findByRole('region', { name: 'Operação registrada' });
    expect(await within(created).findByText('Pendente')).toBeInTheDocument();
    expect(within(created).getByRole('button', { name: 'Liquidar' })).toBeInTheDocument();
    expect(router.state.location.search).toBe('?operacao=0192a000-0000-7000-8000-00000000c001');
    expect(screen.getByText('Nenhum recebível no lote')).toBeInTheDocument();
  });
});
