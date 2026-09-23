import { useQuery } from '@tanstack/react-query';

import { queryKeys } from '@/shared/api/query-keys';

import { fetchCashAccounts } from '../api/cash-accounts-api';

/** Invalidated by every settlement (see the credit-assignments feature). */
export function useCashAccounts() {
  return useQuery({
    queryKey: queryKeys.cashAccounts.all,
    queryFn: ({ signal }) => fetchCashAccounts(signal),
  });
}
