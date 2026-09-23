import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from '@tanstack/react-query';

import { queryKeys } from '@/shared/api/query-keys';

import {
  fetchLatestRate,
  fetchRateHistory,
  registerManualRate,
  synchronizeRates,
  type CurrencyPair,
} from '../api/exchange-rates-api';

export function useLatestRate(pair: CurrencyPair) {
  return useQuery({
    queryKey: queryKeys.exchangeRates.latest(pair.base, pair.quote),
    queryFn: ({ signal }) => fetchLatestRate(pair, signal),
  });
}

export function useRateHistory(pair: CurrencyPair, page: number, size: number) {
  return useQuery({
    queryKey: queryKeys.exchangeRates.history({ ...pair, page, size }),
    queryFn: ({ signal }) => fetchRateHistory({ ...pair, page, size }, signal),
    placeholderData: keepPreviousData,
  });
}

/** A new rate changes the rates on screen and every price simulated with the old one. */
async function refreshRates(queryClient: QueryClient) {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: queryKeys.exchangeRates.all }),
    queryClient.invalidateQueries({ queryKey: queryKeys.pricing.all }),
  ]);
}

export function useRegisterManualRate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: registerManualRate,
    // Field errors are shown next to the inputs.
    meta: { notifyOnError: false },
    onSuccess: () => refreshRates(queryClient),
  });
}

export function useSynchronizeRates() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: synchronizeRates,
    // A provider outage is explained next to the button.
    meta: { notifyOnError: false },
    onSuccess: () => refreshRates(queryClient),
  });
}
