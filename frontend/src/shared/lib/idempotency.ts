/** Format accepted by the backend for the `Idempotency-Key` header. */
const IDEMPOTENCY_KEY_PATTERN = /^[A-Za-z0-9._:-]{1,100}$/;

/**
 * New key for one logical submission. The caller keeps it while retrying the same payload (e.g.
 * after a network error, when it is unknown whether the server created the resource) and only
 * generates a new one when the payload changes or the previous attempt definitely succeeded.
 */
export function newIdempotencyKey(): string {
  return crypto.randomUUID();
}

export function isValidIdempotencyKey(value: string): boolean {
  return IDEMPOTENCY_KEY_PATTERN.test(value);
}
