import type { AssignorSummary } from '@/shared/api/contract';
import { formatCnpj } from '@/shared/lib/cnpj';

export function assignorLabel(assignor: AssignorSummary): string {
  return `${assignor.name} · ${formatCnpj(assignor.document)}`;
}
