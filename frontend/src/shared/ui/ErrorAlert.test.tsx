import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { ApiError } from '@/shared/api/api-error';
import { renderWithProviders } from '@/test/render';

import { ErrorAlert } from './ErrorAlert';

const validationError = new ApiError({
  status: 400,
  code: 'VALIDATION_ERROR',
  title: 'Requisição inválida',
  detail: 'Um ou mais campos são inválidos.',
  correlationId: 'cid-123',
  fieldErrors: [{ field: 'document', message: 'CNPJ inválido' }],
});

describe('ErrorAlert', () => {
  it('shows the problem title, detail, field errors and the support code', async () => {
    renderWithProviders(<ErrorAlert error={validationError} />);

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Requisição inválida');
    expect(alert).toHaveTextContent('Um ou mais campos são inválidos.');
    expect(alert).toHaveTextContent('document: CNPJ inválido');
    expect(alert).toHaveTextContent('cid-123');
    expect(
      screen.getByRole('button', { name: 'Copiar código para o suporte' }),
    ).toBeInTheDocument();
  });

  it('describes unexpected errors generically and offers a retry', async () => {
    const onRetry = vi.fn();
    const { user } = renderWithProviders(
      <ErrorAlert error={new Error('boom')} onRetry={onRetry} hideFieldErrors />,
    );

    expect(await screen.findByRole('alert')).toHaveTextContent('Erro inesperado');
    expect(screen.queryByText('boom')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Tentar novamente' }));
    expect(onRetry).toHaveBeenCalledOnce();
  });
});
