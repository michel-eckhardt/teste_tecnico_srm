import { EmptyState as MantineEmptyState } from '@mantine/core';
import { IconInbox } from '@tabler/icons-react';
import type { ReactNode } from 'react';

interface EmptyStateProps {
  title: string;
  description?: ReactNode;
  icon?: ReactNode;
  action?: ReactNode;
}

export function EmptyState({ title, description, icon, action }: EmptyStateProps) {
  return (
    <MantineEmptyState
      py="xl"
      title={title}
      description={description}
      icon={icon ?? <IconInbox size={32} stroke={1.5} aria-hidden />}
      withIndicatorBackground
    >
      {action ? <MantineEmptyState.Actions>{action}</MantineEmptyState.Actions> : null}
    </MantineEmptyState>
  );
}
