import { Button, Center, Stack, Text, Title } from '@mantine/core';
import { isRouteErrorResponse, useRouteError } from 'react-router';

import { NotFoundPage } from './NotFoundPage';

/** Rendered by the router when a page throws while rendering: the shell stays usable. */
export function RouteErrorPage() {
  const error = useRouteError();
  if (isRouteErrorResponse(error) && error.status === 404) {
    return <NotFoundPage />;
  }
  console.error('Unhandled rendering error', error);

  return (
    <Center mih="60vh">
      <Stack align="center" gap="sm" maw={480} ta="center">
        <Title order={2}>Algo deu errado</Title>
        <Text c="dimmed">
          Um erro inesperado impediu a exibição desta tela. Nenhuma operação foi alterada.
        </Text>
        <Button onClick={() => window.location.reload()}>Recarregar a página</Button>
      </Stack>
    </Center>
  );
}
