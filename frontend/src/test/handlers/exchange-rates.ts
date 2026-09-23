import { http, HttpResponse } from 'msw';

import type { ExchangeRate, ManualExchangeRateRequest } from '@/shared/api/contract';

import { apiUrl } from '../api';
import { brlUsdRate, usdBrlRate } from '../fixtures';

function page(content: ExchangeRate[], number: number, size: number, totalElements: number) {
  return {
    content,
    page: { number, size, totalElements, totalPages: Math.ceil(totalElements / size) },
  };
}

export const exchangeRateHandlers = [
  http.get(apiUrl('/exchange-rates/latest'), ({ request }) => {
    const params = new URL(request.url).searchParams;
    return HttpResponse.json(params.get('base') === 'USD' ? usdBrlRate : brlUsdRate);
  }),
  http.get(apiUrl('/exchange-rates'), ({ request }) => {
    const params = new URL(request.url).searchParams;
    const number = Number(params.get('page') ?? 0);
    const size = Number(params.get('size') ?? 20);
    if (params.get('base') !== 'USD') return HttpResponse.json(page([], number, size, 0));
    // 12 stored USD/BRL rates: the page is generated from the request
    const total = 12;
    const content = Array.from(
      { length: Math.max(0, Math.min(size, total - number * size)) },
      (_, index) => ({
        ...usdBrlRate,
        id: `rate-${number * size + index}`,
        referenceDate: `2026-09-${String(23 - (number * size + index)).padStart(2, '0')}`,
      }),
    );
    return HttpResponse.json(page(content, number, size, total));
  }),
  http.post(apiUrl('/exchange-rates'), async ({ request }) => {
    const body = (await request.json()) as ManualExchangeRateRequest;
    return HttpResponse.json(
      {
        ...usdBrlRate,
        ...body,
        id: 'manual-rate',
        source: 'MANUAL',
        referenceDate: body.referenceDate ?? '2026-09-23',
      },
      { status: 201 },
    );
  }),
  http.post(apiUrl('/exchange-rates/sync'), () => HttpResponse.json([usdBrlRate])),
];
