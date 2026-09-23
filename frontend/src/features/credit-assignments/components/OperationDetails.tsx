import { Group, SimpleGrid, Stack, Table, Text, Title } from '@mantine/core';
import type { ReactNode } from 'react';

import type { CreditAssignment } from '@/shared/api/contract';
import { formatCnpj } from '@/shared/lib/cnpj';
import { formatDate, formatDateTime } from '@/shared/lib/date';
import { addDecimals, formatPercent, formatRate } from '@/shared/lib/decimal';
import { receivableTypeLabel } from '@/shared/lib/labels';
import { MoneyText } from '@/shared/ui/MoneyText';
import { StatusBadge } from '@/shared/ui/StatusBadge';

interface OperationDetailsProps {
  operation: CreditAssignment;
  /** Commands available for the operation (settle / cancel). */
  actions?: ReactNode;
}

/** A credit assignment operation: assignor, status, totals and the priced receivables. */
export function OperationDetails({ operation, actions }: OperationDetailsProps) {
  const currency = operation.paymentCurrency;
  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start" gap="sm">
        <Stack gap={2}>
          <Group gap="xs">
            <Title order={3} size="h4">
              {operation.assignor.name}
            </Title>
            <StatusBadge status={operation.status} />
          </Group>
          <Text size="sm" c="dimmed">
            CNPJ {formatCnpj(operation.assignor.document)} · operação{' '}
            <Text span ff="monospace" inherit>
              {operation.id}
            </Text>
          </Text>
          <Text size="sm" c="dimmed">
            Criada em {formatDateTime(operation.createdAt)}
            {operation.settledAt ? ` · liquidada em ${formatDateTime(operation.settledAt)}` : ''}
            {operation.cancelledAt
              ? ` · cancelada em ${formatDateTime(operation.cancelledAt)}`
              : ''}
          </Text>
        </Stack>
        {actions}
      </Group>

      <SimpleGrid cols={{ base: 1, xs: 3 }} spacing="sm">
        <Total label={`Valor de face (${currency})`}>
          <MoneyText amount={operation.totalFaceValue} currency={currency} />
        </Total>
        <Total label="Deságio">
          <MoneyText amount={operation.totalDiscount} currency={currency} c="red.7" />
        </Total>
        <Total label="Valor líquido a pagar">
          <MoneyText amount={operation.totalNetAmount} currency={currency} fw={700} />
        </Total>
      </SimpleGrid>

      <Table.ScrollContainer minWidth={820}>
        <Table striped verticalSpacing="xs" fz="sm">
          <Table.Caption>
            {operation.receivablesCount} recebível(is). Valor presente e deságio na moeda de face;
            líquido na moeda de pagamento ({currency}).
          </Table.Caption>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Tipo</Table.Th>
              <Table.Th>Vencimento</Table.Th>
              <Table.Th ta="right">Taxa a.m.</Table.Th>
              <Table.Th ta="right">Valor de face</Table.Th>
              <Table.Th ta="right">Valor presente</Table.Th>
              <Table.Th ta="right">Deságio</Table.Th>
              <Table.Th>Câmbio</Table.Th>
              <Table.Th ta="right">Líquido ({currency})</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {operation.receivables.map((receivable) => (
              <Table.Tr key={receivable.id}>
                <Table.Td>{receivableTypeLabel(receivable.receivableType)}</Table.Td>
                <Table.Td>
                  {formatDate(receivable.dueDate)}
                  <Text span c="dimmed" size="xs">
                    {' '}
                    ({receivable.termDays} dias)
                  </Text>
                </Table.Td>
                <Table.Td ta="right">
                  {formatPercent(addDecimals(receivable.baseRate, receivable.spread))}
                </Table.Td>
                <Table.Td ta="right">
                  <MoneyText amount={receivable.faceValue} currency={receivable.faceCurrency} />
                </Table.Td>
                <Table.Td ta="right">
                  <MoneyText amount={receivable.presentValue} currency={receivable.faceCurrency} />
                </Table.Td>
                <Table.Td ta="right">
                  <MoneyText amount={receivable.discount} currency={receivable.faceCurrency} />
                </Table.Td>
                <Table.Td>
                  {receivable.exchangeRate
                    ? `1 ${receivable.exchangeRate.base} = ${formatRate(receivable.exchangeRate.rate)} ${receivable.exchangeRate.quote}`
                    : '—'}
                </Table.Td>
                <Table.Td ta="right">
                  <MoneyText amount={receivable.netAmount} currency={currency} fw={600} />
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Stack>
  );
}

function Total({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <Text size="xs" c="dimmed" tt="uppercase" fw={600}>
        {label}
      </Text>
      <Text size="lg" component="div">
        {children}
      </Text>
    </div>
  );
}
