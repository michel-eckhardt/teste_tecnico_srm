import { ActionIcon, CopyButton, Group, Text, Tooltip } from '@mantine/core';
import { IconCheck, IconCopy } from '@tabler/icons-react';

/** Support reference of a failed request, with a copy-to-clipboard button. */
export function CorrelationId({ value }: { value: string }) {
  return (
    <Group gap={4} wrap="nowrap">
      <Text size="xs" c="dimmed">
        Código para o suporte:{' '}
        <Text span ff="monospace" inherit>
          {value}
        </Text>
      </Text>
      <CopyButton value={value} timeout={1500}>
        {({ copied, copy }) => (
          <Tooltip label={copied ? 'Copiado' : 'Copiar código'} withArrow>
            <ActionIcon
              variant="subtle"
              color={copied ? 'teal' : 'gray'}
              size="sm"
              onClick={copy}
              aria-label="Copiar código para o suporte"
            >
              {copied ? <IconCheck size={14} /> : <IconCopy size={14} />}
            </ActionIcon>
          </Tooltip>
        )}
      </CopyButton>
    </Group>
  );
}
