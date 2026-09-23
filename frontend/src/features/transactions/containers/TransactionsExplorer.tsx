import { Button, Drawer, Paper, Stack } from '@mantine/core';
import { IconFilterOff } from '@tabler/icons-react';
import { useSearchParams } from 'react-router';

import { AssignorPicker, useAssignor } from '@/features/assignors';
import { CreditAssignmentView } from '@/features/credit-assignments';
import { EmptyState } from '@/shared/ui/EmptyState';
import { ErrorAlert } from '@/shared/ui/ErrorAlert';

import { StatementFiltersBar } from '../components/StatementFiltersBar';
import { StatementTable } from '../components/StatementTable';
import { useStatement, useStatementFilters } from '../hooks/useStatement';
import { hasActiveFilters } from '../model/statement-filters';

const OPERATION_PARAM = 'operacao';

/** Settlement statement: URL-synced filters, server-side sorting/pagination and a detail drawer. */
export function TransactionsExplorer() {
  const { filters, changeFilters, changePage, changePageSize, changeSort, reset } =
    useStatementFilters();
  const statement = useStatement(filters);
  const assignor = useAssignor(filters.assignorId ?? null);
  const [searchParams, setSearchParams] = useSearchParams();
  const openOperation = searchParams.get(OPERATION_PARAM);

  function setOpenOperation(id: string | null) {
    setSearchParams((current) => {
      const next = new URLSearchParams(current);
      if (id) next.set(OPERATION_PARAM, id);
      else next.delete(OPERATION_PARAM);
      return next;
    });
  }

  const page = statement.data;
  const empty = page?.content.length === 0;

  return (
    <Stack gap="md">
      <Paper withBorder p="md">
        <StatementFiltersBar
          filters={filters}
          onChange={changeFilters}
          onReset={reset}
          assignorFilter={
            <AssignorPicker
              label="Cedente"
              value={filters.assignorId ? (assignor.data ?? null) : null}
              onChange={(selected) => {
                changeFilters({ assignorId: selected?.id });
              }}
            />
          }
        />
      </Paper>

      {statement.isError ? (
        <ErrorAlert
          error={statement.error}
          title="Não foi possível carregar o extrato"
          onRetry={() => void statement.refetch()}
        />
      ) : null}

      <Paper withBorder p="md">
        {empty ? (
          <EmptyState
            title="Nenhuma operação encontrada"
            description="Nenhuma operação corresponde aos filtros selecionados."
            action={
              hasActiveFilters(filters) ? (
                <Button variant="light" leftSection={<IconFilterOff size={16} />} onClick={reset}>
                  Limpar filtros
                </Button>
              ) : undefined
            }
          />
        ) : (
          <StatementTable
            rows={page?.content ?? []}
            totalElements={page?.page.totalElements ?? 0}
            totalPages={page?.page.totalPages ?? 0}
            page={filters.page}
            size={filters.size}
            sort={filters.sort}
            loading={statement.isPending}
            stale={statement.isPlaceholderData}
            onPageChange={changePage}
            onPageSizeChange={changePageSize}
            onSortChange={changeSort}
            onOpen={setOpenOperation}
          />
        )}
      </Paper>

      <Drawer
        opened={openOperation !== null}
        onClose={() => {
          setOpenOperation(null);
        }}
        position="right"
        size="xl"
        title="Detalhes da operação"
      >
        {openOperation ? <CreditAssignmentView operationId={openOperation} /> : null}
      </Drawer>
    </Stack>
  );
}
