import { Button, Input, SegmentedControl, Select, SimpleGrid, Stack, Text } from '@mantine/core';
import { DateInput } from '@mantine/dates';
import { IconCalendar, IconPlus } from '@tabler/icons-react';
import { Controller, useWatch, type Control } from 'react-hook-form';

import { CURRENCY_CODES, type IsoDate, type ReceivableType } from '@/shared/api/contract';
import { parseBrDate } from '@/shared/lib/date';
import { formatPercent } from '@/shared/lib/decimal';
import { MoneyInput } from '@/shared/ui/MoneyInput';

import type { ReceivableFormValues } from '../model/receivable-form';

const CURRENCY_OPTIONS = CURRENCY_CODES.map((code) => ({ value: code, label: code }));

interface ReceivableFormProps {
  control: Control<ReceivableFormValues>;
  receivableTypes: readonly ReceivableType[];
  receivableTypesLoading: boolean;
  minDueDate: IsoDate;
  maxDueDate: IsoDate;
  /** Payment currency fixed by the batch being assembled. */
  paymentCurrencyLocked: boolean;
  canAddToBatch: boolean;
  onAddToBatch: () => void;
}

/** Receivable data typed by the operator (value, due date, type, currencies). */
export function ReceivableForm({
  control,
  receivableTypes,
  receivableTypesLoading,
  minDueDate,
  maxDueDate,
  paymentCurrencyLocked,
  canAddToBatch,
  onAddToBatch,
}: ReceivableFormProps) {
  const faceCurrency = useWatch({ control, name: 'faceCurrency' });

  return (
    <form
      noValidate
      aria-label="Dados do recebível"
      onSubmit={(event) => {
        event.preventDefault();
        if (canAddToBatch) onAddToBatch();
      }}
    >
      <Stack gap="md">
        <Controller
          control={control}
          name="receivableType"
          render={({ field, fieldState }) => (
            <Select
              label="Tipo de recebível"
              placeholder={receivableTypesLoading ? 'Carregando tipos…' : 'Selecione'}
              withAsterisk
              allowDeselect={false}
              data={receivableTypes.map((type) => ({
                value: type.code,
                label: `${type.description} · spread ${formatPercent(type.monthlySpread)} a.m.`,
              }))}
              disabled={receivableTypesLoading}
              value={field.value || null}
              onChange={(value) => {
                field.onChange(value ?? '');
              }}
              onBlur={field.onBlur}
              error={fieldState.error?.message}
            />
          )}
        />

        <SimpleGrid cols={{ base: 1, xs: 2 }} spacing="md">
          <Controller
            control={control}
            name="faceValue"
            render={({ field, fieldState }) => (
              <MoneyInput
                label="Valor de face"
                withAsterisk
                currency={faceCurrency}
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                error={fieldState.error?.message}
              />
            )}
          />
          <Controller
            control={control}
            name="faceCurrency"
            render={({ field }) => (
              <Input.Wrapper label="Moeda do título" labelElement="div">
                <SegmentedControl
                  fullWidth
                  aria-label="Moeda do título"
                  data={CURRENCY_OPTIONS}
                  value={field.value}
                  onChange={field.onChange}
                />
              </Input.Wrapper>
            )}
          />
        </SimpleGrid>

        <SimpleGrid cols={{ base: 1, xs: 2 }} spacing="md">
          <Controller
            control={control}
            name="dueDate"
            render={({ field, fieldState }) => (
              <DateInput
                label="Vencimento"
                withAsterisk
                placeholder="DD/MM/AAAA"
                valueFormat="DD/MM/YYYY"
                dateParser={parseBrDate}
                minDate={minDueDate}
                maxDate={maxDueDate}
                leftSection={<IconCalendar size={16} aria-hidden />}
                clearable
                value={field.value || null}
                onChange={(value) => {
                  field.onChange(value ?? '');
                }}
                onBlur={field.onBlur}
                error={fieldState.error?.message}
              />
            )}
          />
          <Controller
            control={control}
            name="paymentCurrency"
            render={({ field }) => (
              <Input.Wrapper
                label="Moeda de pagamento"
                labelElement="div"
                description={paymentCurrencyLocked ? 'Definida pelo lote em montagem' : undefined}
              >
                <SegmentedControl
                  fullWidth
                  aria-label="Moeda de pagamento"
                  data={CURRENCY_OPTIONS}
                  value={field.value}
                  onChange={field.onChange}
                  disabled={paymentCurrencyLocked}
                />
              </Input.Wrapper>
            )}
          />
        </SimpleGrid>

        <Button type="submit" leftSection={<IconPlus size={16} />} disabled={!canAddToBatch}>
          Adicionar ao lote
        </Button>
        <Text size="xs" c="dimmed">
          O valor líquido é recalculado automaticamente a cada alteração. A precificação final é
          feita pelo servidor ao registrar a operação.
        </Text>
      </Stack>
    </form>
  );
}
