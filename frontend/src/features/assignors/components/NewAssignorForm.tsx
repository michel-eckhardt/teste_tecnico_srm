import { Button, Group, Stack, TextInput } from '@mantine/core';
import type { ReactNode } from 'react';
import { Controller, type Control } from 'react-hook-form';

import { maskCnpj } from '@/shared/lib/cnpj';

import type { AssignorFormValues } from '../model/assignor-form';

interface NewAssignorFormProps {
  control: Control<AssignorFormValues>;
  submitting: boolean;
  onSubmit: () => void;
  onCancel: () => void;
  /** Error not attributable to a field (network, unexpected...). */
  errorSlot?: ReactNode;
}

export function NewAssignorForm({
  control,
  submitting,
  onSubmit,
  onCancel,
  errorSlot,
}: NewAssignorFormProps) {
  return (
    <form
      noValidate
      aria-label="Novo cedente"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit();
      }}
    >
      <Stack gap="md">
        <Controller
          control={control}
          name="name"
          render={({ field, fieldState }) => (
            <TextInput
              label="Razão social"
              withAsterisk
              maxLength={150}
              data-autofocus
              {...field}
              error={fieldState.error?.message}
            />
          )}
        />
        <Controller
          control={control}
          name="document"
          render={({ field, fieldState }) => (
            <TextInput
              label="CNPJ"
              withAsterisk
              inputMode="numeric"
              placeholder="00.000.000/0000-00"
              {...field}
              onChange={(event) => {
                field.onChange(maskCnpj(event.currentTarget.value));
              }}
              error={fieldState.error?.message}
            />
          )}
        />
        {errorSlot}
        <Group justify="flex-end">
          <Button variant="default" onClick={onCancel} disabled={submitting}>
            Cancelar
          </Button>
          <Button type="submit" loading={submitting}>
            Cadastrar cedente
          </Button>
        </Group>
      </Stack>
    </form>
  );
}
