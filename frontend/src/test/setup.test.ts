import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';

import { apiUrl } from './api';
import { server } from './server';

describe('test environment', () => {
  it('answers API calls through MSW handlers', async () => {
    server.use(http.get(apiUrl('/ping'), () => HttpResponse.json({ ok: true })));

    const response = await fetch(apiUrl('/ping'));

    expect(await response.json()).toEqual({ ok: true });
  });

  it('fails requests that no handler expects, so tests never hit a real server', async () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);

    await expect(fetch(apiUrl('/not-mocked'))).rejects.toThrow();
    consoleError.mockRestore();
  });
});
