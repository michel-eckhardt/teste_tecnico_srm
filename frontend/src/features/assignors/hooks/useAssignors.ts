import {
  keepPreviousData,
  skipToken,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import { queryKeys } from '@/shared/api/query-keys';
import { useDebouncedValue } from '@/shared/hooks/useDebouncedValue';

import { fetchAssignor, registerAssignor, searchAssignors } from '../api/assignors-api';

const SEARCH_PAGE_SIZE = 20;
const SEARCH_DEBOUNCE_MS = 300;

/** Server-side assignor search for select inputs (debounced, previous results kept). */
export function useAssignorSearch(search: string, enabled = true) {
  const term = useDebouncedValue(search.trim(), SEARCH_DEBOUNCE_MS);
  return useQuery({
    queryKey: queryKeys.assignors.search(term, SEARCH_PAGE_SIZE),
    queryFn: enabled
      ? ({ signal }) => searchAssignors({ search: term, size: SEARCH_PAGE_SIZE }, signal)
      : skipToken,
    placeholderData: keepPreviousData,
  });
}

export function useAssignor(id: string | null) {
  return useQuery({
    queryKey: queryKeys.assignors.detail(id ?? ''),
    queryFn: id ? ({ signal }) => fetchAssignor(id, signal) : skipToken,
    staleTime: 5 * 60 * 1000,
  });
}

export function useRegisterAssignor() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: registerAssignor,
    // Errors (duplicate CNPJ, invalid fields) are shown next to the form fields.
    meta: { notifyOnError: false },
    onSuccess: async (assignor) => {
      queryClient.setQueryData(queryKeys.assignors.detail(assignor.id), assignor);
      await queryClient.invalidateQueries({ queryKey: queryKeys.assignors.all });
    },
  });
}
