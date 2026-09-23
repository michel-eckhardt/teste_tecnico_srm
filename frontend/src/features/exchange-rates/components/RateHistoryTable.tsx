import { Badge, Group, Pagination, SegmentedControl, Skeleton, Table, Text } from '@mantine/core';

import type { ExchangeRate } from '@/shared/api/contract';
import { formatDate, formatDateTime } from '@/shared/lib/date';
import { formatRate } from '@/shared/lib/decimal';
import { RATE_SOURCE_LABELS } from '@/shared/lib/labels';
import { EmptyState } from '@/shared/ui/EmptyState';

import { HISTORY_PAIRS, type HistoryPair } from '../model/pairs';

interface RateHistoryTableProps {
  pair: HistoryPair;
  onPairChange: (pair: HistoryPair) => void;
  rows: readonly ExchangeRate[];
  page: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (page: number) => void;
  loading: boolean;
  stale: boolean;
}

/** Stored rates of a pair, most recent first (server-side pagination). */
export function RateHistoryTable({
  pair,
  onPairChange,
  rows,
  page,
  totalPages,
  totalElements,
  onPageChange,
  loading,
  stale,
}: RateHistoryTableProps) {
  return (
    <>
      <Group justify="space-between" mb="sm">
        <SegmentedControl
          aria-label="Par de moedas do histórico"
          data={[...HISTORY_PAIRS]}
          value={pair}
          onChange={(value) => {
            const next = HISTORY_PAIRS.find((option) => option === value);
            if (next) onPairChange(next);
          }}
        />
        <Text size="sm" c="dimmed">
          {totalElements} registro(s)
        </Text>
      </Group>
      {!loading && rows.length === 0 ? (
        <EmptyState
          title={`Nenhuma taxa ${pair} registrada`}
          description="Pares sem registro próprio são derivados do par inverso."
        />
      ) : (
        <Table.ScrollContainer minWidth={520}>
          <Table
            striped
            verticalSpacing="xs"
            fz="sm"
            style={{ opacity: stale ? 0.6 : 1, whiteSpace: 'nowrap' }}
          >
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Referência</Table.Th>
                <Table.Th ta="right">Taxa</Table.Th>
                <Table.Th>Fonte</Table.Th>
                <Table.Th>Registrada em</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {loading
                ? Array.from({ length: 3 }, (_, index) => (
                    <Table.Tr key={index}>
                      <Table.Td colSpan={4}>
                        <Skeleton h={16} />
                      </Table.Td>
                    </Table.Tr>
                  ))
                : rows.map((rate) => (
                    <Table.Tr key={rate.id}>
                      <Table.Td>{formatDate(rate.referenceDate)}</Table.Td>
                      <Table.Td ta="right" style={{ fontVariantNumeric: 'tabular-nums' }}>
                        {formatRate(rate.rate)}
                      </Table.Td>
                      <Table.Td>
                        <Badge variant="light" size="sm">
                          {RATE_SOURCE_LABELS[rate.source]}
                        </Badge>
                      </Table.Td>
                      <Table.Td>{formatDateTime(rate.createdAt)}</Table.Td>
                    </Table.Tr>
                  ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}
      {totalPages > 1 ? (
        <Group justify="flex-end" mt="sm">
          <Pagination
            size="sm"
            total={totalPages}
            value={page + 1}
            onChange={(value) => {
              onPageChange(value - 1);
            }}
            getControlProps={(control) => ({
              'aria-label': control === 'previous' ? 'Página anterior' : 'Próxima página',
            })}
          />
        </Group>
      ) : null}
    </>
  );
}
