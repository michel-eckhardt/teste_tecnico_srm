import { skipToken, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import type { CreateCreditAssignmentRequest } from '@/shared/api/contract';
import { queryKeys } from '@/shared/api/query-keys';

import {
  cancelCreditAssignment,
  createCreditAssignment,
  fetchCreditAssignment,
  settleCreditAssignment,
  type VersionedOperation,
} from '../api/credit-assignments-api';
import { explainCommandError, type OperationCommand } from '../model/command-errors';

/** One operation and its current ETag. */
export function useCreditAssignment(id: string | null) {
  return useQuery({
    queryKey: queryKeys.creditAssignments.detail(id ?? ''),
    queryFn: id ? ({ signal }) => fetchCreditAssignment(id, signal) : skipToken,
  });
}

export function useCreateCreditAssignment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      request,
      idempotencyKey,
    }: {
      request: CreateCreditAssignmentRequest;
      idempotencyKey: string;
    }) => createCreditAssignment(request, idempotencyKey),
    // The batch builder explains failures inline (with what happens on retry).
    meta: { notifyOnError: false },
    onSuccess: async (created) => {
      queryClient.setQueryData<VersionedOperation>(
        queryKeys.creditAssignments.detail(created.operation.id),
        { operation: created.operation, etag: created.etag },
      );
      await queryClient.invalidateQueries({ queryKey: queryKeys.statement.all });
    },
  });
}

/**
 * Settlement / cancellation of an operation, always conditional on the version the operator is
 * looking at (If-Match). When the server says that version is outdated or already final, the
 * operation is reloaded so the screen shows what actually happened.
 */
export function useOperationCommand(id: string) {
  const queryClient = useQueryClient();
  const detailKey = queryKeys.creditAssignments.detail(id);

  return useMutation({
    mutationFn: ({ command, etag }: { command: OperationCommand; etag: string }) =>
      command === 'settle' ? settleCreditAssignment(id, etag) : cancelCreditAssignment(id, etag),
    meta: { notifyOnError: false },
    onSuccess: async (updated) => {
      queryClient.setQueryData<VersionedOperation>(detailKey, updated);
      // Settlement debits the fund cash account; both change the statement.
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.statement.all }),
        queryClient.invalidateQueries({ queryKey: queryKeys.cashAccounts.all }),
      ]);
    },
    onError: async (error, { command }) => {
      if (explainCommandError(error, command).refetch) {
        await queryClient.invalidateQueries({ queryKey: detailKey });
      }
    },
  });
}
