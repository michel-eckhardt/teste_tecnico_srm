import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { describe, expect, it } from 'vitest';

import { useUrlState, type UrlStateCodec } from './useUrlState';

interface Paging {
  page: number;
  q: string;
}

const codec: UrlStateCodec<Paging> = {
  keys: ['page', 'q'],
  parse: (params) => {
    const page = Number(params.get('page'));
    return { page: Number.isInteger(page) && page > 0 ? page : 0, q: params.get('q') ?? '' };
  },
  serialize: ({ page, q }) => ({
    ...(page > 0 ? { page: String(page) } : {}),
    ...(q ? { q } : {}),
  }),
};

function setup(initialUrl: string) {
  let router: ReturnType<typeof createMemoryRouter> | undefined;
  const wrapper = ({ children }: { children: ReactNode }) => {
    router ??= createMemoryRouter([{ path: '*', element: children }], {
      initialEntries: [initialUrl],
    });
    return <RouterProvider router={router} />;
  };
  const hook = renderHook(() => useUrlState(codec), { wrapper });
  return { ...hook, router: () => router! };
}

describe('useUrlState', () => {
  it('reads (and sanitizes) the state from the URL', () => {
    const { result } = setup('/list?page=-3&q=acme');
    expect(result.current[0]).toEqual({ page: 0, q: 'acme' });
  });

  it('writes the state, keeping unrelated params and pushing a history entry', () => {
    const { result, router } = setup('/list?q=acme&operacao=42');

    act(() => {
      result.current[1]((current) => ({ ...current, page: 2 }));
    });

    expect(result.current[0]).toEqual({ page: 2, q: 'acme' });
    const search = new URLSearchParams(router().state.location.search);
    expect(Object.fromEntries(search)).toEqual({ q: 'acme', page: '2', operacao: '42' });
    expect(router().state.historyAction).toBe('PUSH');
  });

  it('removes keys the serializer leaves out and can replace the history entry', () => {
    const { result, router } = setup('/list?page=3&q=acme');

    act(() => {
      result.current[1](() => ({ page: 0, q: '' }), { replace: true });
    });

    expect(router().state.location.search).toBe('');
    expect(router().state.historyAction).toBe('REPLACE');
  });
});
