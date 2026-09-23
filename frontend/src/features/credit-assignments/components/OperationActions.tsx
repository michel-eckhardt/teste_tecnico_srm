import { Button, Group } from '@mantine/core';
import { IconBan, IconCircleCheck } from '@tabler/icons-react';

import type { OperationStatus } from '@/shared/api/contract';

import type { OperationCommand } from '../model/command-errors';

interface OperationActionsProps {
  status: OperationStatus;
  /** Command in flight, if any. */
  pending: OperationCommand | null;
  onSettle: () => void;
  onCancel: () => void;
}

/** Settle / cancel commands; only a pending operation can change state. */
export function OperationActions({ status, pending, onSettle, onCancel }: OperationActionsProps) {
  if (status !== 'PENDING') return null;
  return (
    <Group gap="xs">
      <Button
        color="teal"
        leftSection={<IconCircleCheck size={16} />}
        onClick={onSettle}
        loading={pending === 'settle'}
        disabled={pending !== null}
      >
        Liquidar
      </Button>
      <Button
        variant="light"
        color="red"
        leftSection={<IconBan size={16} />}
        onClick={onCancel}
        loading={pending === 'cancel'}
        disabled={pending !== null}
      >
        Cancelar
      </Button>
    </Group>
  );
}
