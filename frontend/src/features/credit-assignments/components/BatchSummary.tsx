import { Button, Group, Paper, SimpleGrid, Stack, Text } from '@mantine/core';
import { IconSend } from '@tabler/icons-react';
import type { ReactNode } from 'react';

import type { CurrencyCode, DecimalString } from '@/shared/api/contract';
import { MoneyText } from '@/shared/ui/MoneyText';

interface BatchSummaryProps {
  itemsCount: number;
  paymentCurrency: CurrencyCode | null;
  faceTotals: readonly { currency: CurrencyCode; total: DecimalString }[];
  estimatedNet: DecimalString;
  /** Why the batch cannot be submitted yet. */
  blocker: string | null;
  submitting: boolean;
  onSubmit: () => void;
}

export function BatchSummary({
  itemsCount,
  paymentCurrency,
  faceTotals,
  estimatedNet,
  blocker,
  submitting,
  onSubmit,
}: BatchSummaryProps) {
  return (
    <Paper withBorder p="md" radius="md" bg="var(--mantine-color-gray-light)">
      <Stack gap="sm">
        <SimpleGrid cols={{ base: 1, xs: 3 }} spacing="sm">
          <Stat label="Recebíveis">{itemsCount}</Stat>
          <Stat label="Valor de face">
            {faceTotals.length === 0
              ? '—'
              : faceTotals.map(({ currency, total }) => (
                  <MoneyText key={currency} amount={total} currency={currency} display="block" />
                ))}
          </Stat>
          <Stat label={`Líquido estimado${paymentCurrency ? ` (${paymentCurrency})` : ''}`}>
            {paymentCurrency ? (
              <MoneyText amount={estimatedNet} currency={paymentCurrency} fw={700} />
            ) : (
              '—'
            )}
          </Stat>
        </SimpleGrid>
        <Group justify="space-between" align="center" gap="sm">
          <Text size="xs" c="dimmed" maw={420}>
            {blocker ??
              'Os valores finais são recalculados pelo servidor ao registrar a operação, com as taxas vigentes.'}
          </Text>
          <Button
            leftSection={<IconSend size={16} />}
            onClick={onSubmit}
            disabled={blocker !== null}
            loading={submitting}
          >
            Registrar operação
          </Button>
        </Group>
      </Stack>
    </Paper>
  );
}

function Stat({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <Text size="xs" c="dimmed" tt="uppercase" fw={600}>
        {label}
      </Text>
      <Text size="lg" style={{ fontVariantNumeric: 'tabular-nums' }} component="div">
        {children}
      </Text>
    </div>
  );
}
