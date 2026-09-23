import type { Currency, ReceivableType } from '@/shared/api/contract';
import { apiClient, sendForData } from '@/shared/api/http';

export async function fetchCurrencies(signal?: AbortSignal): Promise<Currency[]> {
  return (await sendForData(apiClient.GET('/api/v1/currencies', { signal }))) as Currency[];
}

export async function fetchReceivableTypes(signal?: AbortSignal): Promise<ReceivableType[]> {
  return (await sendForData(
    apiClient.GET('/api/v1/receivable-types', { signal }),
  )) as ReceivableType[];
}
