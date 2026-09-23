import { Badge, Button, Group, Paper, Stack, Text, Title } from '@mantine/core';
import { modals } from '@mantine/modals';
import { IconStack2, IconTrashX } from '@tabler/icons-react';

import { AssignorPicker } from '@/features/assignors';
import { isApiError } from '@/shared/api/api-error';
import { formatMoney } from '@/shared/lib/money';
import { EmptyState } from '@/shared/ui/EmptyState';
import { ErrorAlert } from '@/shared/ui/ErrorAlert';
import { notifySuccess } from '@/shared/ui/notify';

import { BatchSummary } from '../components/BatchSummary';
import { BatchTable } from '../components/BatchTable';
import { useCreateCreditAssignment } from '../hooks/useCreditAssignments';
import {
  batchBlocker,
  estimatedNetTotal,
  faceTotalsByCurrency,
  toCreateRequest,
} from '../model/batch';
import { useBatchStore } from '../model/batch-store';
import { describeFieldPath, explainSubmissionError } from '../model/submission-errors';

interface BatchBuilderProps {
  onCreated: (operationId: string) => void;
}

/** The batch being assembled: assignor, receivables, totals and submission. */
export function BatchBuilder({ onCreated }: BatchBuilderProps) {
  const items = useBatchStore((state) => state.items);
  const assignor = useBatchStore((state) => state.assignor);
  const paymentCurrency = useBatchStore((state) => state.paymentCurrency);
  const setAssignor = useBatchStore((state) => state.setAssignor);
  const removeItem = useBatchStore((state) => state.removeItem);
  const clear = useBatchStore((state) => state.clear);
  const submissionKey = useBatchStore((state) => state.submissionKey);
  const resetSubmissionKey = useBatchStore((state) => state.resetSubmissionKey);
  const create = useCreateCreditAssignment();

  const draft = { assignor, paymentCurrency, items };
  const blocker = batchBlocker(draft);

  function submit() {
    const request = toCreateRequest(draft);
    if (!request) return;
    create.mutate(
      { request, idempotencyKey: submissionKey() },
      {
        onSuccess: ({ operation, replayed }) => {
          clear();
          notifySuccess(
            replayed ? 'Operação já estava registrada' : 'Operação registrada',
            `${operation.assignor.name} · ${formatMoney(operation.totalNetAmount, operation.paymentCurrency)} a pagar`,
          );
          onCreated(operation.id);
        },
        onError: (error) => {
          if (isApiError(error, 'IDEMPOTENCY_KEY_REUSED')) resetSubmissionKey();
        },
      },
    );
  }

  function confirmClear() {
    modals.openConfirmModal({
      title: 'Limpar o lote?',
      children: (
        <Text size="sm">Todos os recebíveis e o cedente selecionado serão descartados.</Text>
      ),
      labels: { confirm: 'Limpar lote', cancel: 'Voltar' },
      confirmProps: { color: 'red' },
      onConfirm: () => {
        clear();
        create.reset();
      },
    });
  }

  const failure = create.isError ? explainSubmissionError(create.error) : null;

  return (
    <Paper withBorder p="md">
      <Stack gap="md">
        <Group justify="space-between">
          <Group gap="xs">
            <Title order={3} size="h4">
              Lote em montagem
            </Title>
            {paymentCurrency ? (
              <Badge variant="light" title="Moeda de pagamento do lote">
                Pagamento em {paymentCurrency}
              </Badge>
            ) : null}
          </Group>
          <Button
            variant="subtle"
            color="red"
            leftSection={<IconTrashX size={16} />}
            onClick={confirmClear}
            disabled={(items.length === 0 && !assignor) || create.isPending}
          >
            Limpar lote
          </Button>
        </Group>

        <AssignorPicker value={assignor} onChange={setAssignor} required allowCreate />

        {paymentCurrency && items.length > 0 ? (
          <BatchTable
            items={items}
            paymentCurrency={paymentCurrency}
            onRemove={removeItem}
            disabled={create.isPending}
          />
        ) : (
          <EmptyState
            icon={<IconStack2 size={32} stroke={1.5} aria-hidden />}
            title="Nenhum recebível no lote"
            description="Simule um recebível acima e clique em “Adicionar ao lote”. A moeda de pagamento do primeiro item vale para o lote todo."
          />
        )}

        {failure ? (
          <ErrorAlert
            error={create.error}
            title={failure.title}
            message={failure.message}
            fieldLabel={describeFieldPath}
          >
            {failure.hint ? <Text size="sm">{failure.hint}</Text> : null}
          </ErrorAlert>
        ) : null}

        <BatchSummary
          itemsCount={items.length}
          paymentCurrency={paymentCurrency}
          faceTotals={faceTotalsByCurrency(items)}
          estimatedNet={estimatedNetTotal(items)}
          blocker={blocker}
          submitting={create.isPending}
          onSubmit={submit}
        />
      </Stack>
    </Paper>
  );
}
