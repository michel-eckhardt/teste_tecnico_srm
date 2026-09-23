import { Stack } from '@mantine/core';

import { CurrentRates, RateHistory } from '@/features/exchange-rates';
import { PageHeader } from '@/shared/ui/PageHeader';

export function ExchangeRatesPage() {
  return (
    <>
      <PageHeader
        title="Câmbio"
        description="Taxas vigentes (publicadas ou derivadas do par inverso), sincronização com a Frankfurter e histórico."
      />
      <Stack gap="lg">
        <CurrentRates />
        <RateHistory />
      </Stack>
    </>
  );
}
