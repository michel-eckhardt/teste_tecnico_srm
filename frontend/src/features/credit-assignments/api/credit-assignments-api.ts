import type { CreateCreditAssignmentRequest, CreditAssignment } from '@/shared/api/contract';
import { apiClient, ETAG_HEADER, send } from '@/shared/api/http';

/** An operation plus the version tag required by the commands that change it (If-Match). */
export interface VersionedOperation {
  operation: CreditAssignment;
  etag: string;
}

function versioned(data: unknown, response: Response): VersionedOperation {
  const operation = data as CreditAssignment;
  // The ETag is the optimistic-locking version; the body carries the same value as a fallback.
  const etag = response.headers.get(ETAG_HEADER) ?? `"${operation.version}"`;
  return { operation, etag };
}

export async function createCreditAssignment(
  request: CreateCreditAssignmentRequest,
  idempotencyKey: string,
): Promise<VersionedOperation & { replayed: boolean }> {
  const { data, response } = await send(
    apiClient.POST('/api/v1/credit-assignments', {
      params: { header: { 'Idempotency-Key': idempotencyKey } },
      body: request,
    }),
  );
  // 201 = created now; 200 = the same key/payload was already processed (safe replay).
  return { ...versioned(data, response), replayed: response.status === 200 };
}
