import { Button, Select, SimpleGrid, Stack, Text, TextInput } from '@mantine/core';
import { DateInput } from '@mantine/dates';
import { IconCalendar, IconDeviceFloppy } from '@tabler/icons-react';
import type { ReactNode } from 'react';
import { Controller, type Control } from 'react-hook-form';

import { CURRENCY_CODES, type CurrencyCode, type IsoDate } from '@/shared/api/contract';
import { parseBrDate } from '@/shared/lib/date';
import { CURRENCY_LABELS } from '@/shared/lib/labels';

import type { ManualRateFormValues } from '../model/manual-rate-form';

const CURRENCY_OPTIONS = CURRENCY_CODES.map((code) => ({
  value: code,
  label: CURRENCY_LABELS[code],
}));

function toCurrency(value: string | null): CurrencyCode | undefined {
  return CURRENCY_CODES.find((code) => code === value);
}

interface ManualRateFormProps {
  control: Control<ManualRateFormValues>;
  maxReferenceDate: IsoDate;
  submitting: boolean;
  onSubmit: () => void;
  errorSlot?: ReactNode;
}

/** Manual exchange rate: units of the quote currency per 1 unit of the base currency. */
export function ManualRateForm({
  control,
  maxReferenceDate,
  submitting,
  onSubmit,
  errorSlot,
}: ManualRateFormProps) {
  return (
    <form
      noValidate
      aria-label="Cadastro manual de taxa"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit();
      }}
    >
      <Stack gap="md">
        <SimpleGrid cols={{ base: 1, xs: 2 }} spacing="md">
          <Controller
            control={control}
            name="base"
            render={({ field, fieldState }) => (
              <Select
                label="Moeda base"
                withAsterisk
                allowDeselect={false}
                data={CURRENCY_OPTIONS}
                value={field.value}
                onChange={(value) => {
                  field.onChange(toCurrency(value) ?? field.value);
                }}
                error={fieldState.error?.message}
              />
            )}
          />
          <Controller
            control={control}
            name="quote"
            render={({ field, fieldState }) => (
              <Select
                label="Moeda cotada"
                withAsterisk
                allowDeselect={false}
                data={CURRENCY_OPTIONS}
                value={field.value}
                onChange={(value) => {
                  field.onChange(toCurrency(value) ?? field.value);
                }}
                error={fieldState.error?.message}
              />
            )}
          />
          <Controller
            control={control}
            name="rate"
            render={({ field, fieldState }) => (
              <TextInput
                label="Taxa"
                description="Unidades da moeda cotada por 1 unidade da base (até 8 casas)"
                placeholder="5,1322"
                withAsterisk
                inputMode="decimal"
                autoComplete="off"
                {...field}
                error={fieldState.error?.message}
              />
            )}
          />
          <Controller
            control={control}
            name="referenceDate"
            render={({ field, fieldState }) => (
              <DateInput
                label="Data de referência"
                description="Opcional: padrão é a data de hoje"
                placeholder="DD/MM/AAAA"
                valueFormat="DD/MM/YYYY"
                dateParser={parseBrDate}
                maxDate={maxReferenceDate}
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
        </SimpleGrid>
        {errorSlot}
        <Button
          type="submit"
          leftSection={<IconDeviceFloppy size={16} />}
          loading={submitting}
          w="fit-content"
        >
          Cadastrar taxa
        </Button>
        <Text size="xs" c="dimmed">
          A taxa passa a valer imediatamente para novas simulações e operações; o histórico é
          mantido (nenhuma taxa é sobrescrita).
        </Text>
      </Stack>
    </form>
  );
}
