import { http, HttpResponse } from 'msw';

import type { Assignor, AssignorRequest } from '@/shared/api/contract';

import { apiUrl } from '../api';
import { acme, globex, problem } from '../fixtures';

const assignors: Assignor[] = [acme, globex];

function page<T>(content: T[], number = 0, size = 20) {
  return {
    content,
    page: { number, size, totalElements: content.length, totalPages: content.length ? 1 : 0 },
  };
}

export const assignorHandlers = [
  http.get(apiUrl('/assignors'), ({ request }) => {
    const search = new URL(request.url).searchParams.get('search')?.toLowerCase() ?? '';
    const matches = assignors.filter(
      (assignor) =>
        assignor.name.toLowerCase().includes(search) || assignor.document.startsWith(search),
    );
    return HttpResponse.json(page(matches));
  }),
  http.get(apiUrl('/assignors/:id'), ({ params }) => {
    const found = assignors.find((assignor) => assignor.id === params.id);
    return found
      ? HttpResponse.json(found)
      : HttpResponse.json(
          problem(404, 'RESOURCE_NOT_FOUND', 'Recurso não encontrado', 'Cedente não encontrado.'),
          { status: 404 },
        );
  }),
  http.post(apiUrl('/assignors'), async ({ request }) => {
    const body = (await request.json()) as AssignorRequest;
    const created: Assignor = {
      id: '0192a000-0000-7000-8000-00000000a0ff',
      name: body.name,
      document: body.document,
      createdAt: '2026-09-23T15:00:00Z',
    };
    return HttpResponse.json(created, { status: 201 });
  }),
];
