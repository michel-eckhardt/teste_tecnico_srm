import { zodResolver } from '@hookform/resolvers/zod';
import { Button, Group, Modal } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { IconUserPlus } from '@tabler/icons-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';

import { isApiError } from '@/shared/api/api-error';
import type { AssignorSummary } from '@/shared/api/contract';
import { applyServerFieldErrors } from '@/shared/lib/form-errors';
import { ErrorAlert } from '@/shared/ui/ErrorAlert';
import { notifySuccess } from '@/shared/ui/notify';

import { AssignorSelect } from '../components/AssignorSelect';
import { NewAssignorForm } from '../components/NewAssignorForm';
import { useAssignorSearch, useRegisterAssignor } from '../hooks/useAssignors';
import {
  assignorFormSchema,
  EMPTY_ASSIGNOR,
  toAssignorRequest,
  type AssignorFormValues,
} from '../model/assignor-form';
import { assignorLabel } from '../model/assignor-label';

interface AssignorPickerProps {
  value: AssignorSummary | null;
  onChange: (assignor: AssignorSummary | null) => void;
  label?: string;
  required?: boolean;
  error?: string | undefined;
  /** Shows the "Novo cedente" action (registration modal). */
  allowCreate?: boolean;
}

/** Assignor selection with server-side search and, optionally, registration of a new one. */
export function AssignorPicker({
  value,
  onChange,
  label = 'Cedente',
  required = false,
  error,
  allowCreate = false,
}: AssignorPickerProps) {
  const [search, setSearch] = useState('');
  // After a selection the input shows the option label: that is not a new search.
  const term = value && search === assignorLabel(value) ? '' : search;
  const results = useAssignorSearch(term);
  const [modalOpened, modal] = useDisclosure(false);

  const select = (
    <AssignorSelect
      label={label}
      required={required}
      error={error}
      value={value}
      onChange={onChange}
      options={results.data?.content ?? []}
      search={search}
      onSearchChange={setSearch}
      loading={results.isFetching}
    />
  );

  if (!allowCreate) return select;

  return (
    <>
      <Group align="flex-end" gap="xs" wrap="nowrap">
        <div style={{ flex: 1 }}>{select}</div>
        <Button variant="light" leftSection={<IconUserPlus size={16} />} onClick={modal.open}>
          Novo cedente
        </Button>
      </Group>
      <Modal opened={modalOpened} onClose={modal.close} title="Novo cedente" centered>
        <NewAssignorDialog
          onCancel={modal.close}
          onCreated={(assignor) => {
            onChange(assignor);
            modal.close();
          }}
        />
      </Modal>
    </>
  );
}

function NewAssignorDialog({
  onCancel,
  onCreated,
}: {
  onCancel: () => void;
  onCreated: (assignor: AssignorSummary) => void;
}) {
  const register = useRegisterAssignor();
  const form = useForm<AssignorFormValues>({
    defaultValues: EMPTY_ASSIGNOR,
    resolver: zodResolver(assignorFormSchema),
  });

  const submit = form.handleSubmit((values) => {
    register.mutate(toAssignorRequest(assignorFormSchema.parse(values)), {
      onSuccess: (assignor) => {
        notifySuccess('Cedente cadastrado', assignor.name);
        onCreated(assignor);
      },
      onError: (error) => {
        if (isApiError(error, 'DUPLICATE_ASSIGNOR')) {
          form.setError('document', { type: 'server', message: error.detail });
          return;
        }
        applyServerFieldErrors(error, form.setError, ['name', 'document']);
      },
    });
  });

  const error = register.error;
  const showGenericError =
    error !== null &&
    !isApiError(error, 'DUPLICATE_ASSIGNOR') &&
    !(isApiError(error, 'VALIDATION_ERROR') && error.fieldErrors.length > 0);

  return (
    <NewAssignorForm
      control={form.control}
      submitting={register.isPending}
      onSubmit={() => void submit()}
      onCancel={onCancel}
      errorSlot={showGenericError ? <ErrorAlert error={error} /> : null}
    />
  );
}
