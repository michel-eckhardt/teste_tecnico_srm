import type { RequestHandler } from 'msw';

import { assignorHandlers } from './assignors';
import { creditAssignmentHandlers } from './credit-assignments';
import { exchangeRateHandlers } from './exchange-rates';
import { pricingHandlers } from './pricing';
import { referenceDataHandlers } from './reference-data';
import { transactionHandlers } from './transactions';

/**
 * Default, contract-shaped handlers (docs/api-contract.md). Tests override them per scenario with
 * `server.use(...)`.
 */
export const handlers: RequestHandler[] = [
  ...referenceDataHandlers,
  ...pricingHandlers,
  ...assignorHandlers,
  ...creditAssignmentHandlers,
  ...transactionHandlers,
  ...exchangeRateHandlers,
];
