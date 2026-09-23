import type { RequestHandler } from 'msw';

import { assignorHandlers } from './assignors';
import { pricingHandlers } from './pricing';
import { referenceDataHandlers } from './reference-data';

/**
 * Default, contract-shaped handlers (docs/api-contract.md). Tests override them per scenario with
 * `server.use(...)`.
 */
export const handlers: RequestHandler[] = [
  ...referenceDataHandlers,
  ...pricingHandlers,
  ...assignorHandlers,
];
