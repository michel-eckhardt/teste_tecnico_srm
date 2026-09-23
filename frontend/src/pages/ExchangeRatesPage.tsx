import { Grid, Stack } from '@mantine/core';

import { CurrentRates, ManualRatePanel, RateHistory } from '@/features/exchange-rates';
import { PageHeader } from '@/shared/ui/PageHeader';

export function ExchangeRatesPage() {
  return (
    <>
      <PageHeader
        title="Câmbio"
        description="Taxas vigentes (publicadas ou derivadas do par inverso), sincronização com a Frankfurter e atualização manual."
      />
      <Stack gap="lg">
        <CurrentRates />
        <Grid gap="md">
          <Grid.Col span={{ base: 12, md: 6 }}>
            <Stack gap="md">
              <ManualRatePanel />
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
