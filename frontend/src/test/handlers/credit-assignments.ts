import { http, HttpResponse } from 'msw';

import { apiUrl } from '../api';
import { cashAccounts, creditAssignmentFor } from '../fixtures';

const etag = (version: number) => ({ ETag: `"${version}"` });

export const creditAssignmentHandlers = [
  http.post(apiUrl('/credit-assignments'), () => {
    const operation = creditAssignmentFor();
    return HttpResponse.json(operation, { status: 201, headers: etag(operation.version) });
  }),
  http.get(apiUrl('/credit-assignments/:id'), ({ params }) => {
    const operation = creditAssignmentFor({ id: String(params.id) });
    return HttpResponse.json(operation, { headers: etag(operation.version) });
  }),
  http.post(apiUrl('/credit-assignments/:id/settlement'), ({ params }) => {
    const operation = creditAssignmentFor({
      id: String(params.id),
      status: 'SETTLED',
      version: 1,
      settledAt: '2026-09-23T14:06:10Z',
    });
    return HttpResponse.json(operation, { headers: etag(operation.version) });
  }),
  http.post(apiUrl('/credit-assignments/:id/cancellation'), ({ params }) => {
    const operation = creditAssignmentFor({
      id: String(params.id),
      status: 'CANCELLED',
      version: 1,
      cancelledAt: '2026-09-23T14:06:10Z',
    });
    return HttpResponse.json(operation, { headers: etag(operation.version) });
  }),
  http.get(apiUrl('/cash-accounts'), () => HttpResponse.json(cashAccounts)),
];
