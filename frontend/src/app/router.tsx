import { createBrowserRouter, type RouteObject } from 'react-router';

import { NotFoundPage } from './errors/NotFoundPage';
import { RouteErrorPage } from './errors/RouteErrorPage';
import { AppLayout } from './layout/AppLayout';
import { PageLoader } from './layout/PageLoader';
import { paths } from './paths';

/** Pages are loaded on demand: each route is its own chunk. */
export const routes: RouteObject[] = [
  {
    element: <AppLayout />,
    errorElement: <RouteErrorPage />,
    hydrateFallbackElement: <PageLoader />,
    children: [
      {
        errorElement: <RouteErrorPage />,
        children: [
          {
            path: paths.operatorPanel,
            lazy: async () => ({
              Component: (await import('@/pages/OperatorPanelPage')).OperatorPanelPage,
            }),
          },
          {
            path: paths.transactions,
            lazy: async () => ({
              Component: (await import('@/pages/TransactionsPage')).TransactionsPage,
            }),
          },
          {
            path: paths.exchangeRates,
            lazy: async () => ({
              Component: (await import('@/pages/ExchangeRatesPage')).ExchangeRatesPage,
            }),
          },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
];

export function createRouter() {
  return createBrowserRouter(routes);
}
