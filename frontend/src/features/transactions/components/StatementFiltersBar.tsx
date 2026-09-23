import { Button, Grid, Select } from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { IconCalendar, IconFilterOff } from '@tabler/icons-react';
import type { ReactNode } from 'react';

import { CURRENCY_CODES, OPERATION_STATUSES } from '@/shared/api/contract';
import { STATUS_LABELS } from '@/shared/lib/labels';

import { hasActiveFilters, type StatementFilters } from '../model/statement-filters';

type FilterPatch = Partial<Pick<StatementFilters, 'from' | 'to' | 'currency' | 'status'>>;

interface StatementFiltersBarProps {
  filters: StatementFilters;
  onChange: (patch: FilterPatch) => void;
  onReset: () => void;
  /** Assignor filter (a connected select owned by the assignors feature). */
  assignorFilter: ReactNode;
}

const CURRENCY_OPTIONS = CURRENCY_CODES.map((code) => ({ value: code, label: code }));
const STATUS_OPTIONS = OPERATION_STATUSES.map((status) => ({
  value: status,
  label: STATUS_LABELS[status],
}));

export function StatementFiltersBar({
  filters,
  onChange,
  onReset,
  assignorFilter,
}: StatementFiltersBarProps) {
  return (
    <Grid gap="sm" align="flex-end">
      <Grid.Col span={{ base: 12, sm: 6, lg: 3 }}>
        <DatePickerInput
          type="range"
          label="Período da operação"
          placeholder="Todo o período"
          valueFormat="DD/MM/YYYY"
          leftSection={<IconCalendar size={16} aria-hidden />}
          clearable
          allowSingleDateInRange
          value={[filters.from ?? null, filters.to ?? null]}
          onChange={([from, to]) => {
            onChange({ from: from ?? undefined, to: to ?? undefined });
          }}
        />
      </Grid.Col>
      <Grid.Col span={{ base: 12, sm: 6, lg: 4 }}>{assignorFilter}</Grid.Col>
      <Grid.Col span={{ base: 6, sm: 4, lg: 'auto' }}>
        <Select
          label="Moeda de pagamento"
          placeholder="Todas"
          clearable
          data={CURRENCY_OPTIONS}
          value={filters.currency ?? null}
          onChange={(value) => {
            onChange({ currency: CURRENCY_CODES.find((code) => code === value) });
          }}
        />
      </Grid.Col>
      <Grid.Col span={{ base: 6, sm: 4, lg: 'auto' }}>
        <Select
          label="Status"
          placeholder="Todos"
          clearable
          data={STATUS_OPTIONS}
          value={filters.status ?? null}
          onChange={(value) => {
            onChange({ status: OPERATION_STATUSES.find((status) => status === value) });
          }}
        />
      </Grid.Col>
      <Grid.Col span={{ base: 12, sm: 4, lg: 'content' }}>
        <Button
          variant="default"
          fullWidth
          leftSection={<IconFilterOff size={16} />}
          onClick={onReset}
          disabled={!hasActiveFilters(filters)}
        >
          Limpar filtros
        </Button>
      </Grid.Col>
    </Grid>
  );
}
