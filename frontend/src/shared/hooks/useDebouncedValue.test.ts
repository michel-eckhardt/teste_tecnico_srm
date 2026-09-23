import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useDebouncedValue } from './useDebouncedValue';

function elapse(ms: number) {
  act(() => {
    vi.advanceTimersByTime(ms);
  });
}

describe('useDebouncedValue', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('publishes the latest value only after it stops changing', () => {
    const { result, rerender } = renderHook(({ value }) => useDebouncedValue(value, 300), {
      initialProps: { value: 'a' },
    });

    rerender({ value: 'ab' });
    elapse(200);
    rerender({ value: 'abc' });
    elapse(200);
    expect(result.current).toBe('a');

    elapse(100);
    expect(result.current).toBe('abc');
  });

  it('treats structurally equal objects as the same value', () => {
    const { result, rerender } = renderHook(({ value }) => useDebouncedValue(value, 300), {
      initialProps: { value: { amount: '10.00' } },
    });
    const first = result.current;

    // new object, same content: no pending update, identity preserved
    rerender({ value: { amount: '10.00' } });
    elapse(1000);
    expect(result.current).toBe(first);

    rerender({ value: { amount: '11.00' } });
    elapse(300);
    expect(result.current).toEqual({ amount: '11.00' });
  });
});
