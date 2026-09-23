import { Badge, type BadgeProps } from '@mantine/core';

import type { OperationStatus } from '@/shared/api/contract';
import { STATUS_LABELS } from '@/shared/lib/labels';

const STATUS_COLORS: Record<OperationStatus, string> = {
  PENDING: 'yellow',
  SETTLED: 'teal',
  CANCELLED: 'gray',
};

interface StatusBadgeProps extends Omit<BadgeProps, 'children' | 'color'> {
  status: OperationStatus;
}

export function StatusBadge({ status, ...props }: StatusBadgeProps) {
  return (
    <Badge color={STATUS_COLORS[status]} variant="light" {...props}>
      {STATUS_LABELS[status]}
    </Badge>
  );
}
