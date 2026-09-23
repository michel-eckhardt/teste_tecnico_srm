import { z } from 'zod';

import type { AssignorRequest } from '@/shared/api/contract';
import { isValidCnpj, normalizeCnpj } from '@/shared/lib/cnpj';

/** Mirrors the backend validation (name up to 150 chars, CNPJ with valid check digits). */
export const assignorFormSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'Informe a razão social.')
    .max(150, 'Use no máximo 150 caracteres.'),
  document: z.string().refine(isValidCnpj, 'CNPJ inválido.'),
});

export type AssignorFormValues = z.input<typeof assignorFormSchema>;

export const EMPTY_ASSIGNOR: AssignorFormValues = { name: '', document: '' };

export function toAssignorRequest(values: z.output<typeof assignorFormSchema>): AssignorRequest {
  return { name: values.name, document: normalizeCnpj(values.document) };
}
