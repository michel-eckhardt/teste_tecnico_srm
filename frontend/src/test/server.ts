import { setupServer } from 'msw/node';

import { handlers } from './handlers';

/** MSW server for tests only: the running app always talks to the real backend. */
export const server = setupServer(...handlers);
