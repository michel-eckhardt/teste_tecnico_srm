import { useMutation, useQueryClient } from '@tanstack/react-query';

import type { CreateCreditAssignmentRequest } from '@/shared/api/contract';
import { queryKeys } from '@/shared/api/query-keys';

import { createCreditAssignment, type VersionedOperation } from '../api/credit-assignments-api';

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
