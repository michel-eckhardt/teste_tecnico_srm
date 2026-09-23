import '@mantine/core/styles.css';
import '@mantine/dates/styles.css';
import '@mantine/notifications/styles.css';
import 'dayjs/locale/pt-br';

import { MantineProvider } from '@mantine/core';
import { DatesProvider } from '@mantine/dates';
import { ModalsProvider } from '@mantine/modals';
import { Notifications } from '@mantine/notifications';
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query';
import type { ReactNode } from 'react';

import { theme } from './theme';

interface AppProvidersProps {
  queryClient: QueryClient;
  /** `test` disables Mantine transitions and portals (deterministic DOM in tests). */
  mantineEnv?: 'default' | 'test';
  children: ReactNode;
}

export function AppProviders({ queryClient, mantineEnv = 'default', children }: AppProvidersProps) {
  return (
    <QueryClientProvider client={queryClient}>
      <MantineProvider theme={theme} defaultColorScheme="light" env={mantineEnv}>
        <DatesProvider settings={{ locale: 'pt-br', firstDayOfWeek: 0 }}>
          <ModalsProvider
            labels={{ confirm: 'Confirmar', cancel: 'Voltar' }}
            modalProps={{ centered: true }}
          >
            <Notifications position="top-right" limit={4} />
            {children}
          </ModalsProvider>
        </DatesProvider>
      </MantineProvider>
    </QueryClientProvider>
  );
}
