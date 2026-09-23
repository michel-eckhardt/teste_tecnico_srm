import { Button, Group, SimpleGrid, Stack, Title } from '@mantine/core';
import { IconRefresh } from '@tabler/icons-react';

import { formatRate } from '@/shared/lib/decimal';
import { ErrorAlert } from '@/shared/ui/ErrorAlert';
import { notifySuccess } from '@/shared/ui/notify';

import { RateCard } from '../components/RateCard';
import { useLatestRate, useSynchronizeRates } from '../hooks/useExchangeRates';
import { BRL_USD, USD_BRL } from '../model/pairs';
import { explainSyncError } from '../model/sync-errors';

/** USD/BRL and BRL/USD in force, and the synchronization with the Frankfurter API. */
export function CurrentRates() {
  const usdBrl = useLatestRate(USD_BRL);
  const brlUsd = useLatestRate(BRL_USD);
  const sync = useSynchronizeRates();

  function synchronize() {
    sync.mutate(undefined, {
      onSuccess: (rates) => {
        notifySuccess(
          'Taxas sincronizadas com a Frankfurter',
          rates.length === 0
            ? 'Nenhuma taxa nova recebida.'
            : rates
                .map((rate) => `${rate.base}/${rate.quote} ${formatRate(rate.rate)}`)
                .join(' · '),
        );
      },
    });
  }

  const failure = sync.isError ? explainSyncError(sync.error) : null;

  return (
    <Stack gap="md">
      <Group justify="space-between">
        <Title order={3} size="h4">
          Taxas vigentes
        </Title>
        <Button
          variant="light"
          leftSection={<IconRefresh size={16} />}
          loading={sync.isPending}
          onClick={synchronize}
        >
          Sincronizar com Frankfurter
        </Button>
      </Group>
      {failure ? (
        <ErrorAlert error={sync.error} title={failure.title} message={failure.message} />
      ) : null}
      <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
        <RateCard
          {...USD_BRL}
          rate={usdBrl.data}
          loading={usdBrl.isPending}
          error={usdBrl.error}
          onRetry={() => void usdBrl.refetch()}
        />
        <RateCard
          {...BRL_USD}
          rate={brlUsd.data}
          loading={brlUsd.isPending}
          error={brlUsd.error}
          onRetry={() => void brlUsd.refetch()}
        />
      </SimpleGrid>
    </Stack>
  );
}
