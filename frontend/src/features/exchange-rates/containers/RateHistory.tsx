import { Paper, Title } from '@mantine/core';
import { useState } from 'react';

import { ErrorAlert } from '@/shared/ui/ErrorAlert';

import { RateHistoryTable } from '../components/RateHistoryTable';
import { useRateHistory } from '../hooks/useExchangeRates';
import { pairOf, type HistoryPair } from '../model/pairs';

const PAGE_SIZE = 10;

export function RateHistory() {
  const [pair, setPair] = useState<HistoryPair>('USD/BRL');
  const [page, setPage] = useState(0);
  const history = useRateHistory(pairOf(pair), page, PAGE_SIZE);

  return (
    <Paper withBorder p="md" component="section" aria-labelledby="rate-history-title">
      <Title id="rate-history-title" order={3} size="h4" mb="sm">
        Histórico de taxas
      </Title>
      {history.isError ? (
        <ErrorAlert error={history.error} onRetry={() => void history.refetch()} />
      ) : (
        <RateHistoryTable
          pair={pair}
          onPairChange={(next) => {
            setPair(next);
            setPage(0);
          }}
          rows={history.data?.content ?? []}
          page={page}
          totalPages={history.data?.page.totalPages ?? 0}
          totalElements={history.data?.page.totalElements ?? 0}
          onPageChange={setPage}
          loading={history.isPending}
          stale={history.isPlaceholderData}
        />
      )}
    </Paper>
  );
}
