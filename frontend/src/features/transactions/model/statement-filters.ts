import { z } from 'zod';

import {
  CURRENCY_CODES,
  OPERATION_STATUSES,
  type CurrencyCode,
  type IsoDate,
  type OperationStatus,
  type StatementSortField,
} from '@/shared/api/contract';
import { isIsoDate } from '@/shared/lib/date';
import type { UrlStateCodec } from '@/shared/hooks/useUrlState';

export const PAGE_SIZES = [10, 20, 50, 100] as const;
export type PageSize = (typeof PAGE_SIZES)[number];

/** Columns the backend accepts in `sort` (whitelist). */
export const SORTABLE_FIELDS = [
  'createdAt',
  'settledAt',
  'totalNetAmount',
  'assignorName',
] as const satisfies readonly StatementSortField[];

export interface StatementSort {
  field: StatementSortField;
  direction: 'asc' | 'desc';
}

export interface StatementFilters {
  from?: IsoDate | undefined;
  to?: IsoDate | undefined;
  assignorId?: string | undefined;
  currency?: CurrencyCode | undefined;
  status?: OperationStatus | undefined;
  /** Zero-based, like the API. */
  page: number;
  size: PageSize;
  sort: StatementSort;
}

export const DEFAULT_SORT: StatementSort = { field: 'createdAt', direction: 'desc' };
export const DEFAULT_PAGE_SIZE: PageSize = 20;

export const DEFAULT_FILTERS: StatementFilters = {
  page: 0,
  size: DEFAULT_PAGE_SIZE,
  sort: DEFAULT_SORT,
};

const isoDate = z.string().refine(isIsoDate);

const sortSchema = z
  .string()
  .regex(/^(createdAt|settledAt|totalNetAmount|assignorName),(asc|desc)$/)
  .transform((value): StatementSort => {
    const [field, direction] = value.split(',') as [StatementSortField, 'asc' | 'desc'];
    return { field, direction };
  });

/**
 * Filters as they may appear in the URL. Every field is validated on its own and an invalid value
 * falls back to its default, so a hand-edited or outdated link still opens the screen.
 */
const urlSchema = z.object({
  from: isoDate.optional().catch(undefined),
  to: isoDate.optional().catch(undefined),
  assignorId: z.uuid().optional().catch(undefined),
  currency: z.enum(CURRENCY_CODES).optional().catch(undefined),
  status: z.enum(OPERATION_STATUSES).optional().catch(undefined),
  page: z.coerce.number().int().min(0).max(1_000_000).catch(0),
  size: z.coerce
    .number()
    .refine((size): size is PageSize => (PAGE_SIZES as readonly number[]).includes(size))
    .catch(DEFAULT_PAGE_SIZE),
  sort: sortSchema.catch(DEFAULT_SORT),
});

export const FILTER_KEYS = [
  'from',
  'to',
  'assignorId',
  'currency',
  'status',
  'page',
  'size',
  'sort',
] as const;

export function parseStatementFilters(params: URLSearchParams): StatementFilters {
  const raw = Object.fromEntries(FILTER_KEYS.map((key) => [key, params.get(key) ?? undefined]));
  const parsed: StatementFilters = urlSchema.parse(raw);
  // An inverted period (hand-edited URL) is read in the order that makes sense.
  if (parsed.from && parsed.to && parsed.from > parsed.to) {
    return { ...parsed, from: parsed.to, to: parsed.from };
  }
  return parsed;
}

/** URL representation: defaults are omitted to keep links short. */
export function serializeStatementFilters(filters: StatementFilters): Record<string, string> {
  const params: Record<string, string> = {};
  if (filters.from) params.from = filters.from;
  if (filters.to) params.to = filters.to;
  if (filters.assignorId) params.assignorId = filters.assignorId;
  if (filters.currency) params.currency = filters.currency;
  if (filters.status) params.status = filters.status;
  if (filters.page > 0) params.page = String(filters.page);
  if (filters.size !== DEFAULT_PAGE_SIZE) params.size = String(filters.size);
  const sort = formatSort(filters.sort);
  if (sort !== formatSort(DEFAULT_SORT)) params.sort = sort;
  return params;
}

export const statementFiltersCodec: UrlStateCodec<StatementFilters> = {
  parse: parseStatementFilters,
  serialize: serializeStatementFilters,
  keys: FILTER_KEYS,
};

export function formatSort(sort: StatementSort): string {
  return `${sort.field},${sort.direction}`;
}

/** Query parameters of GET /reports/settlement-statement. */
export function toStatementQuery(filters: StatementFilters) {
  return {
    from: filters.from,
    to: filters.to,
    assignorId: filters.assignorId,
    currency: filters.currency,
    status: filters.status,
    page: filters.page,
    size: filters.size,
    sort: formatSort(filters.sort),
  };
}

export function hasActiveFilters(filters: StatementFilters): boolean {
  return [filters.from, filters.to, filters.assignorId, filters.currency, filters.status].some(
    (value) => value !== undefined,
  );
}
