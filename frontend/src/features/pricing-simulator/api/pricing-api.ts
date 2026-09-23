import type { Simulation, SimulationRequest } from '@/shared/api/contract';
import { apiClient, sendForData } from '@/shared/api/http';

/** Stateless pricing of one receivable (nothing is persisted). */
export async function simulatePricing(
  request: SimulationRequest,
  signal?: AbortSignal,
): Promise<Simulation> {
  return (await sendForData(
    apiClient.POST('/api/v1/pricing/simulations', { body: request, signal }),
  )) as Simulation;
}
