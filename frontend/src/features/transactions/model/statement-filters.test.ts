import { describe, expect, it } from 'vitest';

import {
  DEFAULT_FILTERS,
  parseStatementFilters,
  serializeStatementFilters,
  toStatementQuery,
} from './statement-filters';

const ASSIGNOR = '0192a000-0000-7000-8000-00000000a001';

describe('statement filters <-> URL', () => {
  it('uses the defaults for an empty URL and keeps default values out of links', () => {
    expect(parseStatementFilters(new URLSearchParams())).toEqual(DEFAULT_FILTERS);
    expect(serializeStatementFilters(DEFAULT_FILTERS)).toEqual({});
  });

  it('round-trips every filter', () => {
    const params = new URLSearchParams({
      from: '2026-09-01',
      to: '2026-09-30',
      assignorId: ASSIGNOR,
      currency: 'USD',
      status: 'SETTLED',
      page: '3',
      size: '50',
      sort: 'totalNetAmount,asc',
    });

    const filters = parseStatementFilters(params);

    expect(filters).toEqual({
      from: '2026-09-01',
      to: '2026-09-30',
      assignorId: ASSIGNOR,
      currency: 'USD',
      status: 'SETTLED',
      page: 3,
      size: 50,
      sort: { field: 'totalNetAmount', direction: 'asc' },
    });
    expect(new URLSearchParams(serializeStatementFilters(filters)).toString()).toBe(
      params.toString(),
    );
  });

  it('drops invalid values instead of failing (hand-edited or outdated links)', () => {
    const filters = parseStatementFilters(
      new URLSearchParams({
        from: '2026-02-30',
        assignorId: 'not-a-uuid',
        currency: 'EUR',
        status: 'LOST',
        page: '-2',
        size: '7',
        sort: 'id,asc',
      }),
    );
    expect(filters).toEqual(DEFAULT_FILTERS);
  });

  it('reads an inverted period in chronological order', () => {
    const filters = parseStatementFilters(
      new URLSearchParams({ from: '2026-09-30', to: '2026-09-01' }),
    );
    expect(filters).toMatchObject({ from: '2026-09-01', to: '2026-09-30' });
  });

  it('builds the API query with the whitelisted sort', () => {
    expect(toStatementQuery({ ...DEFAULT_FILTERS, status: 'PENDING' })).toEqual({
      from: undefined,
      to: undefined,
      assignorId: undefined,
      currency: undefined,
      status: 'PENDING',
      page: 0,
      size: 20,
      sort: 'createdAt,desc',
    });
  });
});
