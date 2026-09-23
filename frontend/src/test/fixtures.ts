import type {
  Assignor,
  CashAccount,
  CreditAssignment,
  Currency,
  ExchangeRate,
  ReceivableType,
  ReceivableTypeCode,
  Simulation,
  SimulationRequest,
} from '@/shared/api/contract';

/** Contract-shaped sample data (docs/api-contract.md). */
export const currencies: Currency[] = [
  { code: 'BRL', name: 'Real brasileiro', decimals: 2 },
  { code: 'USD', name: 'Dólar americano', decimals: 2 },
];

export const receivableTypes: ReceivableType[] = [
  { code: 'CHEQUE_PRE_DATADO', description: 'Cheque Pré-datado', monthlySpread: '0.02500000' },
  { code: 'DUPLICATA_MERCANTIL', description: 'Duplicata Mercantil', monthlySpread: '0.01500000' },
];

export const usdBrlRate: ExchangeRate = {
  id: '0192a000-0000-7000-8000-000000000001',
  base: 'USD',
  quote: 'BRL',
  rate: '5.13220000',
  source: 'FRANKFURTER',
  referenceDate: '2026-09-23',
  createdAt: '2026-09-23T14:05:00Z',
  stale: false,
  derived: false,
};

export const brlUsdRate: ExchangeRate = {
  ...usdBrlRate,
  base: 'BRL',
  quote: 'USD',
  rate: '0.19484821',
  derived: true,
};

export function simulationFor(overrides: Partial<Simulation> = {}): Simulation {
  return {
    receivableType: 'DUPLICATA_MERCANTIL',
    faceValue: '10000.00',
    faceCurrency: 'BRL',
    paymentCurrency: 'BRL',
    operationDate: '2026-09-23',
    dueDate: '2026-12-22',
    termDays: 90,
    termMonths: '3.00000000',
    baseRate: '0.01000000',
    spread: '0.01500000',
    discountRate: '0.02500000',
    presentValue: '9285.99',
    discount: '714.01',
    exchangeRate: null,
    netAmount: '9285.99',
    ...overrides,
  };
}

/** Simulation echoing a request (fixed pricing numbers unless overridden). */
export function simulationForRequest(
  request: SimulationRequest,
  overrides: Partial<Simulation> = {},
): Simulation {
  return simulationFor({
    ...request,
    receivableType: request.receivableType as ReceivableTypeCode,
    ...overrides,
  });
}

export const acme: Assignor = {
  id: '0192a000-0000-7000-8000-00000000a001',
  name: 'ACME Indústria Ltda',
  document: '11222333000181',
  createdAt: '2026-09-01T12:00:00Z',
};

export const globex: Assignor = {
  id: '0192a000-0000-7000-8000-00000000a002',
  name: 'Globex Comércio S.A.',
  document: '11444777000161',
  createdAt: '2026-09-02T12:00:00Z',
};

export function creditAssignmentFor(overrides: Partial<CreditAssignment> = {}): CreditAssignment {
  return {
    id: '0192a000-0000-7000-8000-00000000c001',
    status: 'PENDING',
    version: 0,
    assignor: { id: acme.id, name: acme.name, document: acme.document },
    paymentCurrency: 'BRL',
    totalFaceValue: '22830.50',
    totalDiscount: '1453.20',
    totalNetAmount: '21377.30',
    receivablesCount: 2,
    receivables: [
      {
        id: '0192a000-0000-7000-8000-00000000r001',
        receivableType: 'DUPLICATA_MERCANTIL',
        faceValue: '10000.00',
        faceCurrency: 'BRL',
        dueDate: '2026-12-22',
        termDays: 90,
        baseRate: '0.01000000',
        spread: '0.01500000',
        presentValue: '9285.99',
        discount: '714.01',
        exchangeRate: null,
        netAmount: '9285.99',
      },
      {
        id: '0192a000-0000-7000-8000-00000000r002',
        receivableType: 'CHEQUE_PRE_DATADO',
        faceValue: '2500.00',
        faceCurrency: 'USD',
        dueDate: '2026-11-10',
        termDays: 48,
        baseRate: '0.00500000',
        spread: '0.02500000',
        presentValue: '2379.19',
        discount: '120.81',
        exchangeRate: {
          id: usdBrlRate.id,
          base: 'USD',
          quote: 'BRL',
          rate: '5.13220000',
          referenceDate: '2026-09-23',
        },
        netAmount: '12210.47',
      },
    ],
    createdAt: '2026-09-23T14:05:00Z',
    settledAt: null,
    cancelledAt: null,
    ...overrides,
  };
}

export const cashAccounts: CashAccount[] = [
  { currency: 'BRL', balance: '50000000.00', updatedAt: '2026-09-23T14:05:00Z' },
  { currency: 'USD', balance: '10000000.00', updatedAt: '2026-09-23T14:05:00Z' },
];

/** RFC 9457 body as produced by the backend. */
export function problem(status: number, code: string, title: string, detail: string) {
  return {
    type: `https://srm.com.br/problems/${code.toLowerCase().replaceAll('_', '-')}`,
    title,
    status,
    detail,
    instance: '/api/v1/test',
    code,
    correlationId: `cid-${code.toLowerCase()}`,
  };
}
