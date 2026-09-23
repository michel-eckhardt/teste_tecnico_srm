import { http, HttpResponse } from 'msw';

import { apiUrl } from '../api';
import { statementItemFor } from '../fixtures';

/** A statement of 1.234 operations; each page is generated from the requested page/size. */
export const STATEMENT_TOTAL = 1234;

export const transactionHandlers = [
  http.get(apiUrl('/reports/settlement-statement'), ({ request }) => {
    const params = new URL(request.url).searchParams;
    const page = Number(params.get('page') ?? 0);
    const size = Number(params.get('size') ?? 20);
    const start = page * size;
    const count = Math.max(0, Math.min(size, STATEMENT_TOTAL - start));
    return HttpResponse.json({
      content: Array.from({ length: count }, (_, index) => statementItemFor(start + index + 1)),
      page: {
        number: page,
        size,
        totalElements: STATEMENT_TOTAL,
        totalPages: Math.ceil(STATEMENT_TOTAL / size),
      },
    });
  }),
];
