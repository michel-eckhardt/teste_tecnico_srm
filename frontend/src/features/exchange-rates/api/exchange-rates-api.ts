import type {
  CurrencyCode,
  ExchangeRate,
  ManualExchangeRateRequest,
  Page,
} from '@/shared/api/contract';
import { apiClient, sendForData } from '@/shared/api/http';

export interface CurrencyPair {
  base: CurrencyCode;
  quote: CurrencyCode;
}

/** Rate in force for the pair: the published one or derived from the inverse pair. */
export async function fetchLatestRate(pair: CurrencyPair, signal?: AbortSignal) {
  return (await sendForData(
    apiClient.GET('/api/v1/exchange-rates/latest', { params: { query: pair }, signal }),
  )) as ExchangeRate;
}

/** Stored rates of the pair, most recent first. */
export async function fetchRateHistory(
  { base, quote, page, size }: CurrencyPair & { page: number; size: number },
  signal?: AbortSignal,
) {
  return (await sendForData(
    apiClient.GET('/api/v1/exchange-rates', {
      params: { query: { base, quote, page, size } },
      signal,
    }),
  )) as Page<ExchangeRate>;
}

export async function registerManualRate(request: ManualExchangeRateRequest) {
  return (await sendForData(
    apiClient.POST('/api/v1/exchange-rates', { body: request }),
  )) as ExchangeRate;
}

/** Pulls the current rates from Frankfurter (retry + circuit breaker on the server). */
export async function synchronizeRates() {
  return (await sendForData(apiClient.POST('/api/v1/exchange-rates/sync'))) as ExchangeRate[];
}
