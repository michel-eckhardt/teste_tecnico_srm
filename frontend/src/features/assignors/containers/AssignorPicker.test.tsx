import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { useState } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import type { AssignorRequest, AssignorSummary } from '@/shared/api/contract';
import { apiUrl } from '@/test/api';
import { problem } from '@/test/fixtures';
import { renderWithProviders } from '@/test/render';
import { server } from '@/test/server';

import { AssignorPicker } from './AssignorPicker';

function Harness({ onChange }: { onChange: (assignor: AssignorSummary | null) => void }) {
  const [value, setValue] = useState<AssignorSummary | null>(null);
  return (
    <AssignorPicker
      value={value}
      onChange={(assignor) => {
        setValue(assignor);
        onChange(assignor);
      }}
      allowCreate
    />
  );
}

describe('AssignorPicker', () => {
  afterEach(() => {
    server.events.removeAllListeners();
  });

  it('searches assignors on the server and selects one', async () => {
    const searches: (string | null)[] = [];
    server.events.on('request:start', ({ request }) => {
      const url = new URL(request.url);
      if (url.pathname === '/api/v1/assignors') searches.push(url.searchParams.get('search'));
    });
    const onChange = vi.fn();
    const { user } = renderWithProviders(<Harness onChange={onChange} />);

    await user.type(screen.getByRole('combobox', { name: 'Cedente' }), 'glo');
    // one debounced request for the word (after the initial listing), not one per keystroke
    await waitFor(() => {
      expect(searches).toEqual([null, 'glo']);
    });
    await waitFor(() => {
      expect(screen.queryByRole('option', { name: /ACME/ })).not.toBeInTheDocument();
    });
    await user.click(screen.getByRole('option', { name: /Globex Comércio S\.A\./ }));

    expect(onChange).toHaveBeenLastCalledWith(
      expect.objectContaining({ name: 'Globex Comércio S.A.', document: '11444777000161' }),
    );
    expect(screen.getByRole('combobox', { name: 'Cedente' })).toHaveValue(
      'Globex Comércio S.A. · 11.444.777/0001-61',
    );
  });

  it('registers a new assignor with a masked, validated CNPJ and selects it', async () => {
    let sent: AssignorRequest | undefined;
    server.use(
      http.post(apiUrl('/assignors'), async ({ request }) => {
        sent = (await request.json()) as AssignorRequest;
        return HttpResponse.json(
          { id: 'new-id', ...sent, createdAt: '2026-09-23T15:00:00Z' },
          { status: 201 },
        );
      }),
    );
    const onChange = vi.fn();
    const { user } = renderWithProviders(<Harness onChange={onChange} />);

    await user.click(screen.getByRole('button', { name: 'Novo cedente' }));
    const dialog = await screen.findByRole('dialog', { name: 'Novo cedente' });
    await user.type(within(dialog).getByRole('textbox', { name: /Razão social/ }), 'Nova Ltda');
    const cnpj = within(dialog).getByRole('textbox', { name: /CNPJ/ });
    await user.type(cnpj, '11222333000180');
    expect(cnpj).toHaveValue('11.222.333/0001-80');
    await user.click(within(dialog).getByRole('button', { name: 'Cadastrar cedente' }));
    expect(await within(dialog).findByText('CNPJ inválido.')).toBeInTheDocument();
    expect(sent).toBeUndefined();

    await user.clear(cnpj);
    await user.type(cnpj, '11222333000181');
    await user.click(within(dialog).getByRole('button', { name: 'Cadastrar cedente' }));

    await waitFor(() => {
      expect(onChange).toHaveBeenLastCalledWith(expect.objectContaining({ id: 'new-id' }));
    });
    expect(sent).toEqual({ name: 'Nova Ltda', document: '11222333000181' });
    expect(screen.queryByRole('dialog', { name: 'Novo cedente' })).not.toBeInTheDocument();
  });

  it('shows a duplicate CNPJ (409) on the CNPJ field', async () => {
    server.use(
      http.post(apiUrl('/assignors'), () =>
        HttpResponse.json(
          problem(
            409,
            'DUPLICATE_ASSIGNOR',
            'Cedente já cadastrado',
            'Já existe um cedente cadastrado com o CNPJ 11222333000181.',
          ),
          { status: 409, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );
    const { user } = renderWithProviders(<Harness onChange={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: 'Novo cedente' }));
    const dialog = await screen.findByRole('dialog', { name: 'Novo cedente' });
    await user.type(within(dialog).getByRole('textbox', { name: /Razão social/ }), 'ACME');
    await user.type(within(dialog).getByRole('textbox', { name: /CNPJ/ }), '11222333000181');
    await user.click(within(dialog).getByRole('button', { name: 'Cadastrar cedente' }));

    expect(
      await within(dialog).findByText('Já existe um cedente cadastrado com o CNPJ 11222333000181.'),
    ).toBeInTheDocument();
    expect(within(dialog).getByRole('textbox', { name: /CNPJ/ })).toHaveAttribute(
      'aria-invalid',
      'true',
    );
  });
});
