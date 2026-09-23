import { zodResolver } from '@hookform/resolvers/zod';
import { Grid, Paper, Title } from '@mantine/core';
import { useEffect, useMemo } from 'react';
import { useForm, useWatch } from 'react-hook-form';

import { useReceivableTypes } from '@/features/reference-data';
import type { CurrencyCode } from '@/shared/api/contract';
import { addDays, today as currentDate } from '@/shared/lib/date';
import { receivableTypeLabel } from '@/shared/lib/labels';

import { ReceivableForm } from '../components/ReceivableForm';
import { SimulationBreakdown } from '../components/SimulationBreakdown';
import { usePricingSimulation } from '../hooks/usePricingSimulation';
import {
  createReceivableSchema,
  EMPTY_RECEIVABLE,
  MAX_TERM_DAYS,
  toSimulationRequest,
  type ReceivableFormValues,
  type SimulatedReceivable,
} from '../model/receivable-form';
import { simulationErrorHint } from '../model/simulation-errors';

interface PricingSimulatorProps {
  /** Payment currency imposed by the batch in progress (`null` when the batch is empty). */
  lockedPaymentCurrency: CurrencyCode | null;
  onAddToBatch: (receivable: SimulatedReceivable) => void;
}

/** Receivable form + real-time net amount simulation. */
export function PricingSimulator({ lockedPaymentCurrency, onAddToBatch }: PricingSimulatorProps) {
  const today = currentDate();
  const schema = useMemo(() => createReceivableSchema(today), [today]);
  const receivableTypes = useReceivableTypes();

  const form = useForm<ReceivableFormValues>({
    defaultValues: EMPTY_RECEIVABLE,
    resolver: zodResolver(schema),
    mode: 'onTouched',
  });
  const values = useWatch({ control: form.control });
  const request = toSimulationRequest(values, today);
  const simulation = usePricingSimulation(request);

  useEffect(() => {
    if (lockedPaymentCurrency) {
      form.setValue('paymentCurrency', lockedPaymentCurrency);
    }
  }, [form, lockedPaymentCurrency]);

  const canAddToBatch = request !== null && simulation.isCurrent;

  function addToBatch() {
    if (!request || !simulation.simulation || !simulation.isCurrent) return;
    const typeDescription = receivableTypes.data?.find(
      (type) => type.code === request.receivableType,
    )?.description;
    onAddToBatch({
      ...request,
      receivableTypeLabel: typeDescription ?? receivableTypeLabel(request.receivableType),
      simulation: simulation.simulation,
    });
    // Keep type and currencies: the next receivable of a batch is usually similar.
    form.reset({ ...form.getValues(), faceValue: '', dueDate: '' });
  }

  return (
    <Grid gap="md">
      <Grid.Col span={{ base: 12, md: 6 }}>
        <Paper withBorder p="md" h="100%">
          <Title order={3} size="h4" mb="sm">
            Recebível
          </Title>
          <ReceivableForm
            control={form.control}
            receivableTypes={receivableTypes.data ?? []}
            receivableTypesLoading={receivableTypes.isPending}
            minDueDate={addDays(today, 1)}
            maxDueDate={addDays(today, MAX_TERM_DAYS)}
            paymentCurrencyLocked={lockedPaymentCurrency !== null}
            canAddToBatch={canAddToBatch}
            onAddToBatch={addToBatch}
          />
        </Paper>
      </Grid.Col>
      <Grid.Col span={{ base: 12, md: 6 }}>
        <SimulationBreakdown
          simulation={simulation.simulation}
          hasRequest={request !== null}
          isCalculating={simulation.isCalculating}
          isCurrent={simulation.isCurrent}
          error={simulation.error}
          errorHint={simulationErrorHint(simulation.error)}
          onRetry={simulation.retry}
        />
      </Grid.Col>
    </Grid>
  );
}
