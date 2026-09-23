import { Skeleton, Stack, Text } from '@mantine/core';
import { modals } from '@mantine/modals';

import { formatMoney } from '@/shared/lib/money';
import { ErrorAlert } from '@/shared/ui/ErrorAlert';
import { notifySuccess } from '@/shared/ui/notify';

import { OperationActions } from '../components/OperationActions';
import { OperationDetails } from '../components/OperationDetails';
import { useCreditAssignment, useOperationCommand } from '../hooks/useCreditAssignments';
import { explainCommandError, type OperationCommand } from '../model/command-errors';

interface CreditAssignmentViewProps {
  operationId: string;
}

/**
 * An operation with its settle/cancel commands. Every command carries the ETag of the version on
 * screen (If-Match): if someone changed the operation meanwhile, nothing happens on the server and
 * the fresh version is shown instead.
 */
export function CreditAssignmentView({ operationId }: CreditAssignmentViewProps) {
  const query = useCreditAssignment(operationId);
  const command = useOperationCommand(operationId);

  if (query.isPending) {
    return (
      <Stack gap="sm" aria-busy>
        <Skeleton h={28} w="60%" />
        <Skeleton h={60} />
        <Skeleton h={120} />
      </Stack>
    );
  }
  if (query.isError) {
    return <ErrorAlert error={query.error} onRetry={() => void query.refetch()} />;
  }

  const { operation, etag } = query.data;
  const amount = formatMoney(operation.totalNetAmount, operation.paymentCurrency);

  function confirm(kind: OperationCommand) {
    const settle = kind === 'settle';
    modals.openConfirmModal({
      title: settle ? 'Liquidar operação?' : 'Cancelar operação?',
      children: (
        <Text size="sm">
          {settle
            ? `Serão debitados ${amount} da conta-caixa do fundo em ${operation.paymentCurrency}.`
            : 'A operação será cancelada e não poderá mais ser liquidada.'}
        </Text>
      ),
      labels: { confirm: settle ? 'Liquidar' : 'Cancelar operação', cancel: 'Voltar' },
      confirmProps: { color: settle ? 'teal' : 'red' },
      // The version the operator was looking at when deciding.
      onConfirm: () => {
        command.mutate(
          { command: kind, etag },
          {
            onSuccess: () => {
              notifySuccess(
                settle ? 'Operação liquidada' : 'Operação cancelada',
                settle
                  ? `${amount} debitados do caixa em ${operation.paymentCurrency}.`
                  : undefined,
              );
            },
          },
        );
      },
    });
  }

  const failure = command.isError
    ? explainCommandError(command.error, command.variables.command)
    : null;

  return (
    <Stack gap="md">
      {failure ? (
        <ErrorAlert error={command.error} title={failure.title} message={failure.message} />
      ) : null}
      <OperationDetails
        operation={operation}
        actions={
          <OperationActions
            status={operation.status}
            pending={command.isPending ? command.variables.command : null}
            onSettle={() => {
              confirm('settle');
            }}
            onCancel={() => {
              confirm('cancel');
            }}
          />
        }
      />
    </Stack>
  );
}
