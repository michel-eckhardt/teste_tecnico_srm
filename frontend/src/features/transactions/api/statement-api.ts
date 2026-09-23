import type { Page, StatementItem } from '@/shared/api/contract';
import { apiClient, sendForData } from '@/shared/api/http';

import { toStatementQuery, type StatementFilters } from '../model/statement-filters';

/** One page of the settlement statement (filtered, sorted and paginated by the server). */
export async function fetchStatement(
  filters: StatementFilters,
  signal?: AbortSignal,
): Promise<Page<StatementItem>> {
  return (await sendForData(
    apiClient.GET('/api/v1/reports/settlement-statement', {
      params: { query: toStatementQuery(filters) },
      signal,
    }),
  )) as Page<StatementItem>;
}
