import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactElement } from 'react';
import { createMemoryRouter, RouterProvider, type RouteObject } from 'react-router';

import { AppProviders } from '@/app/providers';
import { createQueryClient } from '@/app/query-client';

interface RenderOptions {
  /** Initial URL (path + search). */
  route?: string;
  /** Route pattern the element is mounted on. */
  path?: string;
}

/** Renders an element with the real providers (Mantine, Query, notifications) and a memory router. */
export function renderWithProviders(
  ui: ReactElement,
  { route = '/', path = '*' }: RenderOptions = {},
) {
  return renderRoutes([{ path, element: ui }], { route });
}

export function renderRoutes(routes: RouteObject[], { route = '/' }: { route?: string } = {}) {
  const queryClient = createQueryClient();
  queryClient.setDefaultOptions({
    ...queryClient.getDefaultOptions(),
    queries: { ...queryClient.getDefaultOptions().queries, retry: false, staleTime: 0 },
  });
  const router = createMemoryRouter(routes, { initialEntries: [route] });
  const user = userEvent.setup();
  const view = render(
    <AppProviders queryClient={queryClient} mantineEnv="test">
      <RouterProvider router={router} />
    </AppProviders>,
  );
  return { ...view, user, router, queryClient };
}
