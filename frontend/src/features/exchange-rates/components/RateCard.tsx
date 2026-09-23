import { Alert, Badge, Group, Paper, Skeleton, Stack, Text } from '@mantine/core';
import { IconAlertTriangle } from '@tabler/icons-react';

import { isApiError } from '@/shared/api/api-error';
import type { CurrencyCode, ExchangeRate } from '@/shared/api/contract';
import { formatDate, formatDateTime } from '@/shared/lib/date';
import { formatRate } from '@/shared/lib/decimal';
import { RATE_SOURCE_LABELS } from '@/shared/lib/labels';
import { ErrorAlert } from '@/shared/ui/ErrorAlert';

interface RateCardProps {
  base: CurrencyCode;
  quote: CurrencyCode;
  rate: ExchangeRate | undefined;
  loading: boolean;
  error: unknown;
  onRetry: () => void;
}

/** Rate in force for one pair, with its source, reference date and staleness. */
export function RateCard({ base, quote, rate, loading, error, onRetry }: RateCardProps) {
  const title = `${base}/${quote}`;
  return (
    <Paper withBorder p="md" h="100%" aria-label={`Taxa vigente ${title}`} component="section">
      <Group justify="space-between" mb="xs">
        <Text fw={700}>{title}</Text>
        {rate ? (
          <Group gap={6}>
            {rate.derived ? (
              <Badge variant="outline" color="gray" title="Calculada a partir do par inverso">
                Derivada de {quote}/{base}
              </Badge>
            ) : null}
            <Badge variant="light">{RATE_SOURCE_LABELS[rate.source]}</Badge>
            {rate.stale ? (
              <Badge color="red" variant="filled">
                Desatualizada
              </Badge>
            ) : (
              <Badge color="teal" variant="light">
                Vigente
              </Badge>
            )}
          </Group>
        ) : null}
      </Group>
      {renderBody()}
    </Paper>
  );

  function renderBody() {
    if (loading) {
      return (
        <Stack gap="xs">
          <Skeleton h={32} w="60%" />
          <Skeleton h={14} w="80%" />
        </Stack>
      );
    }
    if (isApiError(error, 'RESOURCE_NOT_FOUND')) {
      return (
        <Text c="dimmed" size="sm">
          Nenhuma taxa cadastrada para {title}. Sincronize com a Frankfurter ou cadastre uma taxa
          manualmente.
        </Text>
      );
    }
    if (error || !rate) {
      return <ErrorAlert error={error} onRetry={onRetry} />;
    }
    return (
      <Stack gap={6}>
        <Text fz={{ base: 22, sm: 28 }} fw={700} style={{ fontVariantNumeric: 'tabular-nums' }}>
          1 {base} = {formatRate(rate.rate)} {quote}
        </Text>
        <Text size="sm" c="dimmed">
          Referência {formatDate(rate.referenceDate)} · registrada em{' '}
          {formatDateTime(rate.createdAt)}
        </Text>
        {rate.stale ? (
          <Alert
            color="red"
            variant="light"
            icon={<IconAlertTriangle aria-hidden />}
            title="Cotação desatualizada"
          >
            Taxa mais antiga que o limite permitido: operações entre moedas serão recusadas até que
            uma cotação nova seja sincronizada ou cadastrada.
          </Alert>
        ) : null}
      </Stack>
    );
  }
}
