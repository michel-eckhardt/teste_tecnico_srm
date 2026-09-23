import { http, HttpResponse } from 'msw';

import type { SimulationRequest } from '@/shared/api/contract';

import { apiUrl } from '../api';
import { simulationForRequest } from '../fixtures';

/** Echoes the request in a contract-shaped simulation (fixed pricing numbers). */
export const pricingHandlers = [
  http.post(apiUrl('/pricing/simulations'), async ({ request }) => {
    const body = (await request.json()) as SimulationRequest;
    return HttpResponse.json(simulationForRequest(body));
  }),
];
