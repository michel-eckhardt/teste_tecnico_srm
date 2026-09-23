import type { CurrencyPair } from '../api/exchange-rates-api';

export const USD_BRL: CurrencyPair = { base: 'USD', quote: 'BRL' };
export const BRL_USD: CurrencyPair = { base: 'BRL', quote: 'USD' };

export const HISTORY_PAIRS = ['USD/BRL', 'BRL/USD'] as const;
export type HistoryPair = (typeof HISTORY_PAIRS)[number];

export function pairOf(label: HistoryPair): CurrencyPair {
  return label === 'USD/BRL' ? USD_BRL : BRL_USD;
}
