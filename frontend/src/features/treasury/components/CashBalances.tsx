import { Paper, SimpleGrid, Skeleton, Text } from '@mantine/core';

import type { CashAccount } from '@/shared/api/contract';
import { formatDateTime } from '@/shared/lib/date';
import { CURRENCY_LABELS } from '@/shared/lib/labels';
import { MoneyText } from '@/shared/ui/MoneyText';

interface CashBalancesProps {
  accounts: readonly CashAccount[];
  loading: boolean;
}

export function CashBalances({ accounts, loading }: CashBalancesProps) {
  if (loading) {
    return (
      <SimpleGrid cols={{ base: 1, xs: 2 }} spacing="md">
        <Skeleton h={72} />
        <Skeleton h={72} />
      </SimpleGrid>
    );
  }
  return (
    <SimpleGrid cols={{ base: 1, xs: 2 }} spacing="md">
      {accounts.map((account) => (
        <Paper key={account.currency} withBorder p="md" radius="md">
          <Text size="xs" c="dimmed" tt="uppercase" fw={600}>
            {CURRENCY_LABELS[account.currency]}
          </Text>
          <MoneyText amount={account.balance} currency={account.currency} fz={24} fw={700} />
          <Text size="xs" c="dimmed">
            Atualizado em {formatDateTime(account.updatedAt)}
          </Text>
        </Paper>
      ))}
    </SimpleGrid>
  );
}
