import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { renderRoutes } from '@/test/render';

import { routes } from './router';

describe('application shell', () => {
  it('renders the navigation and the operator panel on the home route', async () => {
    renderRoutes(routes, { route: '/' });

    expect(
      await screen.findByRole('heading', { name: 'Painel do operador', level: 2 }),
    ).toBeInTheDocument();
    const nav = screen.getByRole('navigation', { name: 'Navegação principal' });
    expect(nav).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Transações/ })).toHaveAttribute('href', '/transacoes');
    expect(screen.getByRole('link', { name: /Câmbio/ })).toHaveAttribute('href', '/cambio');
  });

  it('navigates between pages', async () => {
    const { user } = renderRoutes(routes, { route: '/' });

    await user.click(await screen.findByRole('link', { name: /Transações/ }));

    expect(
      await screen.findByRole('heading', { name: 'Transações', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Transações/ })).toHaveAttribute('data-active');
  });

  it('shows a not-found page for unknown routes', async () => {
    renderRoutes(routes, { route: '/nao-existe' });

    expect(
      await screen.findByRole('heading', { name: 'Página não encontrada' }),
    ).toBeInTheDocument();
  });
});
