import {
  ActionIcon,
  Box,
  Group,
  Pagination,
  Select,
  Skeleton,
  Table,
  Text,
  Tooltip,
  UnstyledButton,
  VisuallyHidden,
} from '@mantine/core';
import { IconArrowDown, IconArrowUp, IconArrowsSort, IconChevronRight } from '@tabler/icons-react';
import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  useReactTable,
  type SortingState,
  type Updater,
} from '@tanstack/react-table';

import type { StatementItem, StatementSortField } from '@/shared/api/contract';
import { formatCnpj } from '@/shared/lib/cnpj';
import { formatDateTime } from '@/shared/lib/date';
import { MoneyText } from '@/shared/ui/MoneyText';
import { StatusBadge } from '@/shared/ui/StatusBadge';

import {
  DEFAULT_SORT,
  PAGE_SIZES,
  SORTABLE_FIELDS,
  type PageSize,
  type StatementSort,
} from '../model/statement-filters';

const integer = new Intl.NumberFormat('pt-BR');
const column = createColumnHelper<StatementItem>();

const columns = [
  column.accessor('createdAt', {
    header: 'Data da operação',
    sortDescFirst: true,
    cell: (info) => formatDateTime(info.getValue()),
  }),
  column.accessor('assignorName', {
    header: 'Cedente',
    cell: ({ row }) => (
      <>
        <Text size="sm" maw={240} truncate="end" title={row.original.assignorName}>
          {row.original.assignorName}
        </Text>
        <Text size="xs" c="dimmed">
          {formatCnpj(row.original.assignorDocument)}
        </Text>
      </>
    ),
  }),
  column.accessor('status', {
    header: 'Status',
    enableSorting: false,
    // max-content: the badge label ellipsizes, so otherwise the auto table layout squeezes the column
    cell: (info) => <StatusBadge status={info.getValue()} miw="max-content" />,
  }),
  column.accessor('paymentCurrency', { header: 'Moeda', enableSorting: false }),
  column.accessor('receivablesCount', {
    header: 'Recebíveis',
    enableSorting: false,
    meta: { align: 'right' },
  }),
  column.accessor('totalFaceValue', {
    header: 'Valor de face',
    enableSorting: false,
    meta: { align: 'right' },
    cell: ({ row }) => (
      <MoneyText amount={row.original.totalFaceValue} currency={row.original.paymentCurrency} />
    ),
  }),
  column.accessor('totalDiscount', {
    header: 'Deságio',
    enableSorting: false,
    meta: { align: 'right' },
    cell: ({ row }) => (
      <MoneyText amount={row.original.totalDiscount} currency={row.original.paymentCurrency} />
    ),
  }),
  column.accessor('totalNetAmount', {
    header: 'Valor líquido',
    sortDescFirst: true,
    meta: { align: 'right' },
    cell: ({ row }) => (
      <MoneyText
        amount={row.original.totalNetAmount}
        currency={row.original.paymentCurrency}
        fw={600}
      />
    ),
  }),
  column.accessor('settledAt', {
    header: 'Liquidada em',
    sortDescFirst: true,
    cell: (info) => {
      const settledAt = info.getValue();
      return settledAt ? formatDateTime(settledAt) : '—';
    },
  }),
];

function isSortField(id: string): id is StatementSortField {
  return (SORTABLE_FIELDS as readonly string[]).includes(id);
}

interface StatementTableProps {
  rows: readonly StatementItem[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: PageSize;
  sort: StatementSort;
  /** First load: no rows to show yet. */
  loading: boolean;
  /** Rows on screen belong to the previous request while the next one loads. */
  stale: boolean;
  onPageChange: (page: number) => void;
  onPageSizeChange: (size: PageSize) => void;
  onSortChange: (sort: StatementSort) => void;
  onOpen: (operationId: string) => void;
}

/**
 * Statement grid: TanStack Table in manual mode, i.e. it only renders what the server returned
 * and turns header clicks and page changes into new server requests (sorting only on the
 * columns the backend whitelists).
 */
export function StatementTable({
  rows,
  totalElements,
  totalPages,
  page,
  size,
  sort,
  loading,
  stale,
  onPageChange,
  onPageSizeChange,
  onSortChange,
  onOpen,
}: StatementTableProps) {
  const sorting: SortingState = [{ id: sort.field, desc: sort.direction === 'desc' }];

  // TanStack Table returns non-memoizable functions: the React Compiler leaves this component alone.
  // eslint-disable-next-line react-hooks/incompatible-library
  const table = useReactTable({
    data: rows as StatementItem[],
    columns,
    getRowId: (row) => row.operationId,
    getCoreRowModel: getCoreRowModel(),
    manualSorting: true,
    manualPagination: true,
    enableMultiSort: false,
    enableSortingRemoval: false,
    rowCount: totalElements,
    pageCount: totalPages,
    state: { sorting, pagination: { pageIndex: page, pageSize: size } },
    onSortingChange: (updater: Updater<SortingState>) => {
      const [first] = typeof updater === 'function' ? updater(sorting) : updater;
      onSortChange(
        first && isSortField(first.id)
          ? { field: first.id, direction: first.desc ? 'desc' : 'asc' }
          : DEFAULT_SORT,
      );
    },
    onPaginationChange: (updater) => {
      const next =
        typeof updater === 'function' ? updater({ pageIndex: page, pageSize: size }) : updater;
      if (next.pageSize !== size) onPageSizeChange(next.pageSize as PageSize);
      else if (next.pageIndex !== page) onPageChange(next.pageIndex);
    },
  });

  const first = totalElements === 0 ? 0 : page * size + 1;
  const last = Math.min((page + 1) * size, totalElements);

  return (
    <Box>
      <Table.ScrollContainer minWidth={1100}>
        <Table
          striped
          highlightOnHover
          verticalSpacing="xs"
          fz="sm"
          aria-busy={loading || stale}
          style={{
            opacity: stale ? 0.6 : 1,
            transition: 'opacity 150ms ease',
            whiteSpace: 'nowrap',
          }}
        >
          <Table.Thead>
            {table.getHeaderGroups().map((group) => (
              <Table.Tr key={group.id}>
                {group.headers.map((header) => {
                  const sorted = header.column.getIsSorted();
                  const align = header.column.columnDef.meta?.align;
                  const label = flexRender(header.column.columnDef.header, header.getContext());
                  return (
                    <Table.Th
                      key={header.id}
                      ta={align}
                      aria-sort={
                        sorted === 'asc'
                          ? 'ascending'
                          : sorted === 'desc'
                            ? 'descending'
                            : undefined
                      }
                    >
                      {header.column.getCanSort() ? (
                        <UnstyledButton
                          onClick={header.column.getToggleSortingHandler()}
                          fw={600}
                          fz="sm"
                        >
                          <Group
                            gap={4}
                            wrap="nowrap"
                            justify={align === 'right' ? 'flex-end' : undefined}
                          >
                            {label}
                            <SortIcon sorted={sorted} />
                          </Group>
                        </UnstyledButton>
                      ) : (
                        label
                      )}
                    </Table.Th>
                  );
                })}
                <Table.Th>
                  <VisuallyHidden>Detalhes</VisuallyHidden>
                </Table.Th>
              </Table.Tr>
            ))}
          </Table.Thead>
          <Table.Tbody>
            {loading
              ? Array.from({ length: 5 }, (_, index) => (
                  <Table.Tr key={`skeleton-${index}`}>
                    {Array.from({ length: columns.length + 1 }, (__, cell) => (
                      <Table.Td key={cell}>
                        <Skeleton h={16} />
                      </Table.Td>
                    ))}
                  </Table.Tr>
                ))
              : table.getRowModel().rows.map((row) => (
                  <Table.Tr
                    key={row.id}
                    style={{ cursor: 'pointer' }}
                    onClick={() => {
                      onOpen(row.id);
                    }}
                  >
                    {row.getVisibleCells().map((cell) => (
                      <Table.Td key={cell.id} ta={cell.column.columnDef.meta?.align}>
                        {flexRender(cell.column.columnDef.cell, cell.getContext())}
                      </Table.Td>
                    ))}
                    <Table.Td>
                      <Tooltip label="Ver detalhes" withArrow>
                        <ActionIcon
                          variant="subtle"
                          aria-label={`Ver detalhes da operação de ${row.original.assignorName}`}
                          onClick={(event) => {
                            event.stopPropagation();
                            onOpen(row.id);
                          }}
                        >
                          <IconChevronRight size={16} />
                        </ActionIcon>
                      </Tooltip>
                    </Table.Td>
                  </Table.Tr>
                ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>

      <Group justify="space-between" mt="md" gap="sm">
        <Text size="sm" c="dimmed" aria-live="polite">
          {loading
            ? 'Carregando…'
            : `Mostrando ${integer.format(first)}–${integer.format(last)} de ${integer.format(totalElements)} operações`}
        </Text>
        <Group gap="sm">
          <Select
            aria-label="Operações por página"
            w={88}
            size="xs"
            allowDeselect={false}
            data={PAGE_SIZES.map((option) => ({ value: String(option), label: String(option) }))}
            value={String(size)}
            onChange={(value) => {
              if (value) table.setPageSize(Number(value));
            }}
          />
          <Pagination
            size="sm"
            total={table.getPageCount()}
            value={page + 1}
            onChange={(value) => {
              table.setPageIndex(value - 1);
            }}
            siblings={1}
            boundaries={1}
            getControlProps={(control) => ({
              'aria-label':
                control === 'previous'
                  ? 'Página anterior'
                  : control === 'next'
                    ? 'Próxima página'
                    : control === 'first'
                      ? 'Primeira página'
                      : 'Última página',
            })}
            getItemProps={(item) => ({ 'aria-label': `Página ${item}` })}
          />
        </Group>
      </Group>
    </Box>
  );
}

function SortIcon({ sorted }: { sorted: false | 'asc' | 'desc' }) {
  // The direction is announced through aria-sort on the header cell.
  if (sorted === 'asc') return <IconArrowUp size={14} aria-hidden />;
  if (sorted === 'desc') return <IconArrowDown size={14} aria-hidden />;
  return <IconArrowsSort size={14} opacity={0.4} aria-hidden />;
}
