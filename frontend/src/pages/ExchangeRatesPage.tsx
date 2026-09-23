import { Grid, Stack } from '@mantine/core';

import { CurrentRates, ManualRatePanel, RateHistory } from '@/features/exchange-rates';
import { CashBalancesCard } from '@/features/treasury';
import { PageHeader } from '@/shared/ui/PageHeader';

export function ExchangeRatesPage() {
  return (
    <>
      <PageHeader
        title="Câmbio"
        description="Taxas vigentes (publicadas ou derivadas do par inverso), sincronização com a Frankfurter, atualização manual e saldos do fundo."
      />
      <Stack gap="lg">
        <CurrentRates />
        <Grid gap="md">
          <Grid.Col span={{ base: 12, md: 6 }}>
            <Stack gap="md">
              <ManualRatePanel />
              <CashBalancesCard />
            </Stack>
          </Grid.Col>
          <Grid.Col span={{ base: 12, md: 6 }}>
            <RateHistory />
          </Grid.Col>
        </Grid>
      </Stack>
    </>
  );
}
