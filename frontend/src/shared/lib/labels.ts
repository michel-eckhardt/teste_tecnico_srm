import type { CurrencyCode, ExchangeRateSource, OperationStatus } from '@/shared/api/contract';

export const STATUS_LABELS: Record<OperationStatus, string> = {
  PENDING: 'Pendente',
  SETTLED: 'Liquidada',
  CANCELLED: 'Cancelada',
};

export const CURRENCY_LABELS: Record<CurrencyCode, string> = {
  BRL: 'Real (BRL)',
  USD: 'Dólar (USD)',
};

export const RATE_SOURCE_LABELS: Record<ExchangeRateSource, string> = {
  MANUAL: 'Manual',
  FRANKFURTER: 'Frankfurter (BCE)',
  SEED: 'Carga inicial',
};
