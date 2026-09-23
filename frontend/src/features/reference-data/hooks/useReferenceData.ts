import { useQuery } from '@tanstack/react-query';

import { queryKeys } from '@/shared/api/query-keys';

import { fetchCurrencies, fetchReceivableTypes } from '../api/reference-data-api';

/** Reference data changes with deployments, not during a session. */
const REFERENCE_STALE_TIME = 60 * 60 * 1000;

export function useCurrencies() {
  return useQuery({
    queryKey: queryKeys.referenceData.currencies(),
    queryFn: ({ signal }) => fetchCurrencies(signal),
    staleTime: REFERENCE_STALE_TIME,
  });
}

/** Receivable types with the monthly spread of their pricing strategy. */
export function useReceivableTypes() {
  return useQuery({
    queryKey: queryKeys.referenceData.receivableTypes(),
    queryFn: ({ signal }) => fetchReceivableTypes(signal),
    staleTime: REFERENCE_STALE_TIME,
    select: (types) => [...types].sort((a, b) => a.description.localeCompare(b.description)),
  });
}
