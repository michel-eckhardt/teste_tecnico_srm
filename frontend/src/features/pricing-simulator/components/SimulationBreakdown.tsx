import { Box, Divider, Group, Loader, Paper, Stack, Table, Text, Title } from '@mantine/core';
import { IconCalculator } from '@tabler/icons-react';
import type { ReactNode } from 'react';

import type { Simulation } from '@/shared/api/contract';
import { formatDate } from '@/shared/lib/date';
import { formatDecimal, formatPercent, formatRate } from '@/shared/lib/decimal';
import { EmptyState } from '@/shared/ui/EmptyState';
import { ErrorAlert } from '@/shared/ui/ErrorAlert';
import { MoneyText } from '@/shared/ui/MoneyText';

interface SimulationBreakdownProps {
  simulation: Simulation | undefined;
  /** The form holds a complete, valid receivable. */
  hasRequest: boolean;
  isCalculating: boolean;
  /** The result shown belongs to the current form values (not a previous one kept on screen). */
  isCurrent: boolean;
  error: unknown;
  errorHint?: string | undefined;
  onRetry: () => void;
}

/** Real-time pricing breakdown: rates, present value, discount, FX and the net amount. */
export function SimulationBreakdown({
  simulation,
  hasRequest,
  isCalculating,
  isCurrent,
  error,
  errorHint,
  onRetry,
}: SimulationBreakdownProps) {
  return (
    <Paper withBorder p="md" h="100%" aria-busy={isCalculating}>
      <Group justify="space-between" mb="sm">
        <Title order={3} size="h4">
          Simulação
        </Title>
        {isCalculating ? <Loader size="xs" aria-label="Calculando simulação" /> : null}
      </Group>
      {renderBody()}
    </Paper>
  );

  function renderBody() {
    if (!hasRequest) {
      return (
        <EmptyState
          icon={<IconCalculator size={32} stroke={1.5} aria-hidden />}
          title="Nenhum recebível para simular"
          description="Preencha tipo, valor de face e vencimento: o valor líquido aparece aqui em tempo real."
        />
      );
    }
    if (error) {
      return (
        <ErrorAlert error={error} onRetry={onRetry}>
          {errorHint ? <Text size="sm">{errorHint}</Text> : null}
        </ErrorAlert>
      );
    }
    if (!simulation) {
      return (
        <Text c="dimmed" size="sm">
          Calculando…
        </Text>
      );
    }
    return (
      <Box
        style={{ opacity: isCurrent ? 1 : 0.55, transition: 'opacity 150ms ease' }}
        aria-live="polite"
      >
        <SimulationDetails simulation={simulation} />
      </Box>
    );
  }
}

function SimulationDetails({ simulation }: { simulation: Simulation }) {
  const rate = simulation.exchangeRate;
  return (
    <Stack gap="sm">
      <Paper bg="var(--mantine-primary-color-light)" p="md" radius="md">
        <Text size="sm" c="dimmed">
          Valor líquido a pagar ({simulation.paymentCurrency})
        </Text>
        <MoneyText
          amount={simulation.netAmount}
          currency={simulation.paymentCurrency}
          fz={28}
          fw={700}
          data-testid="net-amount"
        />
      </Paper>

      <Table variant="vertical" layout="fixed" withRowBorders={false} fz="sm">
        <Table.Tbody>
          <Row label="Valor de face">
            <MoneyText amount={simulation.faceValue} currency={simulation.faceCurrency} />
          </Row>
          <Row label="Prazo">
            {simulation.termDays} dias ({formatDecimal(simulation.termMonths)} meses) · vence em{' '}
            {formatDate(simulation.dueDate)}
          </Row>
          <Row label="Taxa base (a.m.)">{formatPercent(simulation.baseRate)}</Row>
          <Row label="Spread do tipo (a.m.)">{formatPercent(simulation.spread)}</Row>
          <Row label="Taxa de desconto (a.m.)">{formatPercent(simulation.discountRate)}</Row>
          <Row label="Valor presente">
            <MoneyText amount={simulation.presentValue} currency={simulation.faceCurrency} />
          </Row>
          <Row label="Deságio">
            <MoneyText amount={simulation.discount} currency={simulation.faceCurrency} c="red.7" />
          </Row>
          <Row label="Câmbio">
            {rate ? (
              <>
                1 {rate.base} = {formatRate(rate.rate)} {rate.quote}
                <Text span c="dimmed" size="xs">
                  {' '}
                  · referência {formatDate(rate.referenceDate)}
                </Text>
              </>
            ) : (
              <Text span c="dimmed" inherit>
                Não se aplica (mesma moeda)
              </Text>
            )}
          </Row>
        </Table.Tbody>
      </Table>

      <Divider />
      <Text size="xs" c="dimmed">
        Valor presente = valor de face ÷ (1 + taxa base + spread)
        <sup>prazo em meses</sup>
        {rate ? '; a conversão cambial é aplicada ao final.' : '.'}
      </Text>
    </Stack>
  );
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <Table.Tr>
      <Table.Th w="45%" fw={500} c="dimmed">
        {label}
      </Table.Th>
      <Table.Td style={{ fontVariantNumeric: 'tabular-nums' }}>{children}</Table.Td>
    </Table.Tr>
  );
}
