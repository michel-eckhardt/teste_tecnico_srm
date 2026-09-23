import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useCallback } from 'react';

import { queryKeys } from '@/shared/api/query-keys';
import { useDebouncedValue } from '@/shared/hooks/useDebouncedValue';
import { useUrlState } from '@/shared/hooks/useUrlState';

import { fetchStatement } from '../api/statement-api';
import {
  DEFAULT_FILTERS,
  statementFiltersCodec,
  type PageSize,
  type StatementFilters,
  type StatementSort,
} from '../model/statement-filters';

/** Quick successive filter changes (e.g. picking both ends of a period) make one request. */
const FILTER_DEBOUNCE_MS = 250;

type FilterPatch = Partial<Omit<StatementFilters, 'page' | 'size' | 'sort'>>;

/**
 * Statement filters, page and sort, kept in the URL. Changing a filter, the sort or the page size
 * goes back to the first page (the old page number would point somewhere else).
 */
export function useStatementFilters() {
  const [filters, setFilters] = useUrlState(statementFiltersCodec);

  const changeFilters = useCallback(
    (patch: FilterPatch) => {
      setFilters((current) => ({ ...current, ...patch, page: 0 }));
    },
    [setFilters],
  );
  const changePage = useCallback(
    (page: number) => {
      setFilters((current) => ({ ...current, page }));
    },
    [setFilters],
  );
  const changePageSize = useCallback(
    (size: PageSize) => {
      setFilters((current) => ({ ...current, size, page: 0 }));
    },
    [setFilters],
  );
  const changeSort = useCallback(
    (sort: StatementSort) => {
      setFilters((current) => ({ ...current, sort, page: 0 }));
    },
    [setFilters],
  );
  const reset = useCallback(() => {
    setFilters(() => DEFAULT_FILTERS);
  }, [setFilters]);

  return { filters, changeFilters, changePage, changePageSize, changeSort, reset };
}

/** The statement page for the filters; the previous page stays visible while the next loads. */
export function useStatement(filters: StatementFilters) {
  const debounced = useDebouncedValue(filters, FILTER_DEBOUNCE_MS);
  return useQuery({
    queryKey: queryKeys.statement.page(debounced),
    queryFn: ({ signal }) => fetchStatement(debounced, signal),
    placeholderData: keepPreviousData,
  });
}
