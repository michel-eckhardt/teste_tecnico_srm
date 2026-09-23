import { ActionIcon, Table, Text, Tooltip, VisuallyHidden } from '@mantine/core';
import { IconTrash } from '@tabler/icons-react';

import type { CurrencyCode } from '@/shared/api/contract';
import { formatDate } from '@/shared/lib/date';
import { MoneyText } from '@/shared/ui/MoneyText';

import type { BatchItem } from '../model/batch-store';

interface BatchTableProps {
  items: readonly BatchItem[];
  paymentCurrency: CurrencyCode;
  onRemove: (id: string) => void;
  disabled?: boolean;
}

/** Receivables of the batch with the pricing simulated when each was added. */
export function BatchTable({
  items,
  paymentCurrency,
  onRemove,
  disabled = false,
}: BatchTableProps) {
  return (
    <Table.ScrollContainer minWidth={600}>
      <Table striped highlightOnHover verticalSpacing="xs" fz="sm">
        <Table.Thead>
          <Table.Tr>
            <Table.Th>#</Table.Th>
            <Table.Th>Tipo</Table.Th>
            <Table.Th>Vencimento</Table.Th>
            <Table.Th ta="right">Valor de face</Table.Th>
            <Table.Th ta="right">Deságio (simulado)</Table.Th>
            <Table.Th ta="right">Líquido estimado ({paymentCurrency})</Table.Th>
            <Table.Th>
              <VisuallyHidden>Ações</VisuallyHidden>
            </Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {items.map((item, index) => (
            <Table.Tr key={item.id}>
              <Table.Td>{index + 1}</Table.Td>
              <Table.Td>{item.receivableTypeLabel}</Table.Td>
              <Table.Td>
                {formatDate(item.dueDate)}
                <Text span c="dimmed" size="xs">
                  {' '}
                  ({item.preview.termDays} dias)
                </Text>
              </Table.Td>
              <Table.Td ta="right">
                <MoneyText amount={item.faceValue} currency={item.faceCurrency} />
              </Table.Td>
              <Table.Td ta="right">
                <MoneyText amount={item.preview.discount} currency={item.faceCurrency} />
              </Table.Td>
              <Table.Td ta="right">
                <MoneyText amount={item.preview.netAmount} currency={paymentCurrency} fw={600} />
              </Table.Td>
              <Table.Td ta="right">
                <Tooltip label="Remover do lote" withArrow>
                  <ActionIcon
                    variant="subtle"
                    color="red"
                    disabled={disabled}
                    aria-label={`Remover recebível ${index + 1} do lote`}
                    onClick={() => {
                      onRemove(item.id);
                    }}
                  >
                    <IconTrash size={16} />
                  </ActionIcon>
                </Tooltip>
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
