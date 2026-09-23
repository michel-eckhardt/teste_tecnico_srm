import { describe, expect, it } from 'vitest';

import { isValidIdempotencyKey, newIdempotencyKey } from './idempotency';

describe('idempotency keys', () => {
  it('generates unique keys in the format accepted by the backend', () => {
    const first = newIdempotencyKey();
    const second = newIdempotencyKey();
    expect(first).not.toBe(second);
    expect(isValidIdempotencyKey(first)).toBe(true);
  });

  it('validates the header format', () => {
    expect(isValidIdempotencyKey('op:2026.09-23_1')).toBe(true);
    expect(isValidIdempotencyKey('')).toBe(false);
    expect(isValidIdempotencyKey('with space')).toBe(false);
    expect(isValidIdempotencyKey('x'.repeat(101))).toBe(false);
  });
});
