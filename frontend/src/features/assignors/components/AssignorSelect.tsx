import { Select } from '@mantine/core';
import { IconSearch } from '@tabler/icons-react';

import type { AssignorSummary } from '@/shared/api/contract';

import { assignorLabel } from '../model/assignor-label';

interface AssignorSelectProps {
  label: string;
  placeholder?: string;
  description?: string;
  required?: boolean;
  error?: string | undefined;
  value: AssignorSummary | null;
  onChange: (assignor: AssignorSummary | null) => void;
  /** Current page of server-side search results. */
  options: readonly AssignorSummary[];
  search: string;
  onSearchChange: (search: string) => void;
  loading: boolean;
}

/** Searchable assignor select; filtering happens on the server (name or CNPJ prefix). */
export function AssignorSelect({
  label,
  placeholder = 'Buscar por razão social ou CNPJ',
  description,
  required = false,
  error,
  value,
  onChange,
  options,
  search,
  onSearchChange,
  loading,
}: AssignorSelectProps) {
  const byId = new Map(options.map((option) => [option.id, option]));
  if (value) byId.set(value.id, value);
  const data = [...byId.values()].map((assignor) => ({
    value: assignor.id,
    label: assignorLabel(assignor),
  }));

  return (
    <Select
      label={label}
      placeholder={placeholder}
      description={description}
      withAsterisk={required}
      error={error}
      searchable
      clearable
      data={data}
      // results already come filtered from the server
      filter={({ options: all }) => all}
      value={value?.id ?? null}
      onChange={(id) => {
        onChange(id ? (byId.get(id) ?? null) : null);
      }}
      searchValue={search}
      onSearchChange={onSearchChange}
      nothingFoundMessage={loading ? 'Buscando…' : 'Nenhum cedente encontrado'}
      leftSection={<IconSearch size={16} aria-hidden />}
      maxDropdownHeight={280}
    />
  );
}
