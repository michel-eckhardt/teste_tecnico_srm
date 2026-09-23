import type { Assignor, AssignorRequest, Page } from '@/shared/api/contract';
import { apiClient, sendForData } from '@/shared/api/http';

export interface AssignorSearch {
  search: string;
  page?: number;
  size?: number;
}

/** Assignors whose name contains, or whose CNPJ starts with, the search text. */
export async function searchAssignors(
  { search, page = 0, size = 20 }: AssignorSearch,
  signal?: AbortSignal,
): Promise<Page<Assignor>> {
  return (await sendForData(
    apiClient.GET('/api/v1/assignors', {
      params: { query: { search: search || undefined, page, size } },
      signal,
    }),
  )) as Page<Assignor>;
}

export async function fetchAssignor(id: string, signal?: AbortSignal): Promise<Assignor> {
  return (await sendForData(
    apiClient.GET('/api/v1/assignors/{id}', { params: { path: { id } }, signal }),
  )) as Assignor;
}

export async function registerAssignor(request: AssignorRequest): Promise<Assignor> {
  return (await sendForData(apiClient.POST('/api/v1/assignors', { body: request }))) as Assignor;
}
