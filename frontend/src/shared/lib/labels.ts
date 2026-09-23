import type {
  CurrencyCode,
  ExchangeRateSource,
  OperationStatus,
  ReceivableTypeCode,
} from '@/shared/api/contract';

export const STATUS_LABELS: Record<OperationStatus, string> = {
  PENDING: 'Pendente',
  SETTLED: 'Liquidada',
  CANCELLED: 'Cancelada',
};

export const CURRENCY_LABELS: Record<CurrencyCode, string> = {
  BRL: 'Real (BRL)',
  USD: 'Dólar (USD)',
};

/**
 * Display names of the receivable types. Typed by the generated OpenAPI enum: a new type in the
 * backend fails the build here until it gets a label.
 */
export const RECEIVABLE_TYPE_LABELS: Record<ReceivableTypeCode, string> = {
  DUPLICATA_MERCANTIL: 'Duplicata mercantil',
  CHEQUE_PRE_DATADO: 'Cheque pré-datado',
};

export function receivableTypeLabel(code: string): string {
  return code in RECEIVABLE_TYPE_LABELS ? RECEIVABLE_TYPE_LABELS[code as ReceivableTypeCode] : code;
}

export const RATE_SOURCE_LABELS: Record<ExchangeRateSource, string> = {
  MANUAL: 'Manual',
  FRANKFURTER: 'Frankfurter (BCE)',
  SEED: 'Carga inicial',
};
