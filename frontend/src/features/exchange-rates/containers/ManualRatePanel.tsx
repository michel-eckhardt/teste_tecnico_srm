import { zodResolver } from '@hookform/resolvers/zod';
import { Paper, Title } from '@mantine/core';
import { useMemo } from 'react';
import { useForm } from 'react-hook-form';

import { isApiError } from '@/shared/api/api-error';
import { today as currentDate } from '@/shared/lib/date';
import { formatRate } from '@/shared/lib/decimal';
import { ErrorAlert } from '@/shared/ui/ErrorAlert';
import { notifySuccess } from '@/shared/ui/notify';

import { ManualRateForm } from '../components/ManualRateForm';
import { useRegisterManualRate } from '../hooks/useExchangeRates';
import {
  createManualRateSchema,
  EMPTY_MANUAL_RATE,
  formFieldOf,
  toManualRateRequest,
  type ManualRateFormValues,
} from '../model/manual-rate-form';

export function ManualRatePanel() {
  const today = currentDate();
  const schema = useMemo(() => createManualRateSchema(today), [today]);
  const register = useRegisterManualRate();
  const form = useForm<ManualRateFormValues>({
    defaultValues: EMPTY_MANUAL_RATE,
    resolver: zodResolver(schema),
  });

  const submit = form.handleSubmit((values) => {
    register.mutate(toManualRateRequest(values), {
      onSuccess: (rate) => {
        notifySuccess(
          'Taxa cadastrada',
          `1 ${rate.base} = ${formatRate(rate.rate)} ${rate.quote} (vigente para novas operações)`,
        );
        form.reset({ ...values, rate: '', referenceDate: '' });
      },
      onError: (error) => {
        if (!isApiError(error)) return;
        for (const violation of error.fieldErrors) {
          const field = formFieldOf(violation.field);
          if (field) form.setError(field, { type: 'server', message: violation.message });
        }
      },
    });
  });

  const error = register.error;
  const fieldsOnly =
    isApiError(error, 'VALIDATION_ERROR') &&
    error.fieldErrors.every((violation) => formFieldOf(violation.field) !== undefined);

  return (
    <Paper withBorder p="md" component="section" aria-labelledby="manual-rate-title">
      <Title id="manual-rate-title" order={3} size="h4" mb="sm">
        Atualização manual
      </Title>
      <ManualRateForm
        control={form.control}
        maxReferenceDate={today}
        submitting={register.isPending}
        onSubmit={() => void submit()}
        errorSlot={error && !fieldsOnly ? <ErrorAlert error={error} /> : null}
      />
    </Paper>
  );
}
