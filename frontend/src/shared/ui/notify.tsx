import { notifications } from '@mantine/notifications';
import { IconAlertTriangle, IconCheck, IconX } from '@tabler/icons-react';

import { toApiError } from '@/shared/api/api-error';

import { CorrelationId } from './CorrelationId';

/** Toast for a failed action, always carrying the correlation id when there is one. */
export function notifyError(error: unknown, title?: string) {
  const apiError = toApiError(error);
  notifications.show({
    color: 'red',
    icon: <IconX size={18} />,
    title: title ?? apiError.title,
    autoClose: 10_000,
    message: (
      <>
        {apiError.detail}
        {apiError.correlationId ? <CorrelationId value={apiError.correlationId} /> : null}
      </>
    ),
  });
}

export function notifySuccess(title: string, message?: string) {
  notifications.show({ color: 'teal', icon: <IconCheck size={18} />, title, message });
}

export function notifyWarning(title: string, message?: string) {
  notifications.show({ color: 'yellow', icon: <IconAlertTriangle size={18} />, title, message });
}
