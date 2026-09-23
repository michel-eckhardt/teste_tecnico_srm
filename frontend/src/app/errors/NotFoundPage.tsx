import { Button, Center, Stack, Text, Title } from '@mantine/core';
import { Link } from 'react-router';

export function NotFoundPage() {
  return (
    <Center mih="60vh">
      <Stack align="center" gap="sm" maw={480} ta="center">
        <Title order={2}>Página não encontrada</Title>
        <Text c="dimmed">O endereço acessado não existe.</Text>
        <Button component={Link} to="/">
          Voltar ao painel
        </Button>
      </Stack>
    </Center>
  );
}
