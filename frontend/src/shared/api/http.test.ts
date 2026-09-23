import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { apiUrl } from '@/test/api';
import { server } from '@/test/server';

import { ApiError } from './api-error';
import { apiClient, send, sendForData } from './http';

const currencies = [
  { code: 'BRL', name: 'Real brasileiro', decimals: 2 },
  { code: 'USD', name: 'Dólar americano', decimals: 2 },
];

describe('send', () => {
  it('returns the body and the raw response (for headers such as ETag)', async () => {
    server.use(
      http.get(apiUrl('/currencies'), () =>
        HttpResponse.json(currencies, { headers: { ETag: '"3"' } }),
      ),
    );

    const { data, response } = await send(apiClient.GET('/api/v1/currencies'));

    expect(data).toEqual(currencies);
    expect(response.headers.get('ETag')).toBe('"3"');
  });

  it('sends a correlation id with every request', async () => {
    let correlationId: string | null = null;
    server.use(
      http.get(apiUrl('/currencies'), ({ request }) => {
        correlationId = request.headers.get('X-Correlation-Id');
        return HttpResponse.json(currencies);
      }),
    );

    await sendForData(apiClient.GET('/api/v1/currencies'));

    expect(correlationId).toMatch(/^[0-9a-f-]{36}$/);
  });

  it('rejects with an ApiError built from the problem details', async () => {
    server.use(
      http.post(apiUrl('/pricing/simulations'), () =>
        HttpResponse.json(
          {
            title: 'Taxa de câmbio desatualizada',
            status: 422,
            detail: 'A taxa USD/BRL de 2026-09-10 está desatualizada.',
            code: 'EXCHANGE_RATE_STALE',
            correlationId: 'cid-422',
          },
          { status: 422, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );

    const call = sendForData(
      apiClient.POST('/api/v1/pricing/simulations', {
        body: {
          receivableType: 'DUPLICATA_MERCANTIL',
          faceValue: '10000.00',
          faceCurrency: 'BRL',
          dueDate: '2026-12-22',
          paymentCurrency: 'USD',
        },
      }),
    );

    await expect(call).rejects.toBeInstanceOf(ApiError);
    await expect(call).rejects.toMatchObject({
      status: 422,
      code: 'EXCHANGE_RATE_STALE',
      correlationId: 'cid-422',
    });
  });

  it('rejects with an ApiError for error responses without a body', async () => {
    server.use(http.get(apiUrl('/cash-accounts'), () => new HttpResponse(null, { status: 503 })));

    await expect(sendForData(apiClient.GET('/api/v1/cash-accounts'))).rejects.toMatchObject({
      status: 503,
      code: 'INTERNAL_ERROR',
    });
  });

  it('turns network failures into NETWORK_ERROR', async () => {
    server.use(http.get(apiUrl('/currencies'), () => HttpResponse.error()));

    await expect(sendForData(apiClient.GET('/api/v1/currencies'))).rejects.toMatchObject({
      status: 0,
      code: 'NETWORK_ERROR',
    });
  });

  it('lets aborts through untouched so queries can be cancelled', async () => {
    server.use(
      http.get(apiUrl('/currencies'), async () => {
        await new Promise((resolve) => setTimeout(resolve, 50));
        return HttpResponse.json(currencies);
      }),
    );
    const controller = new AbortController();

    const call = sendForData(apiClient.GET('/api/v1/currencies', { signal: controller.signal }));
    controller.abort();

    await expect(call).rejects.toMatchObject({ name: 'AbortError' });
  });
});
