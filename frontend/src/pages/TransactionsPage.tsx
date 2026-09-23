import { TransactionsExplorer } from '@/features/transactions';
import { PageHeader } from '@/shared/ui/PageHeader';

export function TransactionsPage() {
  return (
    <>
      <PageHeader
        title="Transações"
        description="Extrato de liquidação: filtros, ordenação e paginação processados no servidor. Clique em uma operação para ver os detalhes, liquidar ou cancelar."
      />
      <TransactionsExplorer />
    </>
  );
}
