import { Alert, Button, List, Stack, Text } from '@mantine/core';
import { IconAlertTriangle } from '@tabler/icons-react';
import type { ReactNode } from 'react';

import { toApiError } from '@/shared/api/api-error';

import { CorrelationId } from './CorrelationId';

interface ErrorAlertProps {
  error: unknown;
  /** Overrides the title that comes from the problem details. */
  title?: string;
  onRetry?: () => void;
  /** Replaces the detail sent by the server (e.g. a message specific to the screen). */
  message?: ReactNode;
  /** Hides the per-field list when the fields are already shown next to the inputs. */
  hideFieldErrors?: boolean;
  /** Human name of a field path reported by the server. */
  fieldLabel?: (field: string) => string;
  /** Extra guidance rendered below the message. */
  children?: ReactNode;
}

/**
 * Inline error panel for a failed request: the backend's Portuguese title/detail, the rejected
 * fields and the correlation id to quote to support.
 */
export function ErrorAlert({
  error,
  title,
  message,
  onRetry,
  hideFieldErrors = false,
  fieldLabel = (field) => field,
  children,
}: ErrorAlertProps) {
  const apiError = toApiError(error);
  return (
    <Alert
      color="red"
      variant="light"
      title={title ?? apiError.title}
      icon={<IconAlertTriangle aria-hidden />}
      role="alert"
    >
      <Stack gap="xs">
        <Text size="sm">{message ?? apiError.detail}</Text>
        {children}
        {!hideFieldErrors && apiError.fieldErrors.length > 0 ? (
          <List size="sm" spacing={2}>
            {apiError.fieldErrors.map((violation) => (
              <List.Item key={`${violation.field}:${violation.message}`}>
                <Text span ff="monospace" size="xs">
                  {fieldLabel(violation.field)}
                </Text>
                : {violation.message}
              </List.Item>
            ))}
          </List>
        ) : null}
        {apiError.correlationId ? <CorrelationId value={apiError.correlationId} /> : null}
        {onRetry ? (
          <Button size="xs" variant="light" color="red" onClick={onRetry} w="fit-content">
            Tentar novamente
          </Button>
        ) : null}
      </Stack>
    </Alert>
  );
}
