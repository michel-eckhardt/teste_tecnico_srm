import { CloseButton, Group, Paper, Stack, Title } from '@mantine/core';
import { useSearchParams } from 'react-router';

import {
  BatchBuilder,
  CreditAssignmentView,
  useBatchComposer,
} from '@/features/credit-assignments';
import { PricingSimulator } from '@/features/pricing-simulator';
import { PageHeader } from '@/shared/ui/PageHeader';

const OPERATION_PARAM = 'operacao';

/** The registered operation appears below the batch: bring it to the operator. */
function scrollIntoView(node: HTMLElement | null) {
  node?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

export function OperatorPanelPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const operationId = searchParams.get(OPERATION_PARAM);
  const { lockedPaymentCurrency, addReceivable } = useBatchComposer();

  return (
    <>
      <PageHeader
        title="Painel do operador"
        description="Simule o deságio de recebíveis em tempo real, monte o lote e registre a operação de cessão."
      />
      <Stack gap="lg">
        <PricingSimulator
          lockedPaymentCurrency={lockedPaymentCurrency}
          onAddToBatch={addReceivable}
        />
        <BatchBuilder
          onCreated={(id) => {
            setSearchParams({ [OPERATION_PARAM]: id });
          }}
        />
        {operationId ? (
          <Paper
            key={operationId}
            ref={scrollIntoView}
            withBorder
            p="md"
            component="section"
            aria-labelledby="created-operation-title"
          >
            <Group justify="space-between" mb="sm">
              <Title id="created-operation-title" order={3} size="h4">
                Operação registrada
              </Title>
              <CloseButton
                aria-label="Fechar operação"
                onClick={() => {
                  setSearchParams({});
                }}
              />
            </Group>
            <CreditAssignmentView operationId={operationId} />
          </Paper>
        ) : null}
      </Stack>
    </>
  );
}
