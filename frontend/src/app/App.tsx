import { useState } from 'react';
import { RouterProvider } from 'react-router';

import { AppErrorBoundary } from './errors/AppErrorBoundary';
import { AppProviders } from './providers';
import { createQueryClient } from './query-client';
import { createRouter } from './router';

export function App() {
  const [queryClient] = useState(createQueryClient);
  const [router] = useState(createRouter);

  return (
    <AppErrorBoundary>
      <AppProviders queryClient={queryClient}>
        <RouterProvider router={router} />
      </AppProviders>
    </AppErrorBoundary>
  );
}
