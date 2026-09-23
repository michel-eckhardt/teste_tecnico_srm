import { CurrentRates } from '@/features/exchange-rates';
import { PageHeader } from '@/shared/ui/PageHeader';

export function ExchangeRatesPage() {
  return (
    <>
      <PageHeader
        title="Câmbio"
        description="Taxas vigentes (publicadas ou derivadas do par inverso) e sincronização com a Frankfurter."
      />
      <CurrentRates />
    </>
  );
}
