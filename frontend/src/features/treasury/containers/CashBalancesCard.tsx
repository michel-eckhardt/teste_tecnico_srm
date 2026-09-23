import { Paper, Title } from '@mantine/core';

import { ErrorAlert } from '@/shared/ui/ErrorAlert';

import { CashBalances } from '../components/CashBalances';
import { useCashAccounts } from '../hooks/useCashAccounts';

/** Fund cash available in each currency for settlements. */
export function CashBalancesCard() {
  const accounts = useCashAccounts();
  return (
    <Paper withBorder p="md" component="section" aria-labelledby="cash-balances-title">
      <Title id="cash-balances-title" order={3} size="h4" mb="sm">
        Caixa do fundo
      </Title>
      {accounts.isError ? (
        <ErrorAlert error={accounts.error} onRetry={() => void accounts.refetch()} />
      ) : (
        <CashBalances accounts={accounts.data ?? []} loading={accounts.isPending} />
      )}
    </Paper>
  );
}
