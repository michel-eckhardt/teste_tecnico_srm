import { keepPreviousData, skipToken, useQuery } from '@tanstack/react-query';

import type { SimulationRequest } from '@/shared/api/contract';
import { queryKeys } from '@/shared/api/query-keys';
import { useDebouncedValue } from '@/shared/hooks/useDebouncedValue';

import { simulatePricing } from '../api/pricing-api';

/** Wait for the operator to pause typing before asking the server. */
export const SIMULATION_DEBOUNCE_MS = 300;

/**
 * Real-time pricing of the receivable being typed: the (valid) request is debounced, cached per
 * payload, and the previous result stays on screen while the next one loads (no flicker).
 * Invalid/incomplete input never reaches the API.
 */
export function usePricingSimulation(request: SimulationRequest | null) {
  const debounced = useDebouncedValue(request, SIMULATION_DEBOUNCE_MS);
  const settled = JSON.stringify(debounced) === JSON.stringify(request);

  const query = useQuery({
    queryKey: queryKeys.pricing.simulation(debounced),
    queryFn: debounced ? ({ signal }) => simulatePricing(debounced, signal) : skipToken,
    placeholderData: keepPreviousData,
  });

  const hasRequest = request !== null;
  return {
    simulation: hasRequest ? query.data : undefined,
    error: hasRequest && settled ? query.error : null,
    /** A newer result is on its way (typing, debouncing or fetching). */
    isCalculating: hasRequest && (!settled || query.isFetching),
    /** The result on screen belongs to the values currently in the form. */
    isCurrent: hasRequest && settled && query.isSuccess && !query.isPlaceholderData,
    retry: () => void query.refetch(),
  };
}
