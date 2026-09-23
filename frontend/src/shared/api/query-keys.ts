import type { CurrencyCode, SimulationRequest } from './contract';

const referenceData = ['reference-data'] as const;
const assignors = ['assignors'] as const;
const creditAssignments = ['credit-assignments'] as const;
const statement = ['settlement-statement'] as const;
const exchangeRates = ['exchange-rates'] as const;

/**
 * Single source of TanStack Query keys. Keys are hierarchical, so invalidating a prefix (e.g.
 * `queryKeys.statement.all`) refreshes every page/filter combination below it. Living in `shared`
 * lets one feature invalidate another's data (a settlement refreshes the statement and the cash
 * balances) without importing its internals.
 */
export const queryKeys = {
  referenceData: {
    all: referenceData,
    currencies: () => [...referenceData, 'currencies'] as const,
    receivableTypes: () => [...referenceData, 'receivable-types'] as const,
  },
  pricing: {
    simulation: (request: SimulationRequest) => ['pricing-simulation', request] as const,
  },
  assignors: {
    all: assignors,
    search: (search: string, size: number) => [...assignors, 'search', { search, size }] as const,
    detail: (id: string) => [...assignors, 'detail', id] as const,
  },
  creditAssignments: {
    all: creditAssignments,
    detail: (id: string) => [...creditAssignments, 'detail', id] as const,
  },
  statement: {
    all: statement,
    page: (params: object) => [...statement, params] as const,
  },
  exchangeRates: {
    all: exchangeRates,
    latest: (base: CurrencyCode, quote: CurrencyCode) =>
      [...exchangeRates, 'latest', { base, quote }] as const,
    history: (params: { base: CurrencyCode; quote: CurrencyCode; page: number; size: number }) =>
      [...exchangeRates, 'history', params] as const,
  },
  cashAccounts: {
    all: ['cash-accounts'] as const,
  },
};
