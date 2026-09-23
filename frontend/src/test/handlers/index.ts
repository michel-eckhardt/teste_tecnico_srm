import type { RequestHandler } from 'msw';

/**
 * Default, contract-shaped handlers (docs/api-contract.md). Tests override them per scenario with
 * `server.use(...)`.
 */
export const handlers: RequestHandler[] = [];
