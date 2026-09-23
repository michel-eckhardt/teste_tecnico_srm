import type { CashAccount } from '@/shared/api/contract';
import { apiClient, sendForData } from '@/shared/api/http';

/** Balance of the fund cash account in each currency (debited by settlements). */
export async function fetchCashAccounts(signal?: AbortSignal): Promise<CashAccount[]> {
  return (await sendForData(apiClient.GET('/api/v1/cash-accounts', { signal }))) as CashAccount[];
}
