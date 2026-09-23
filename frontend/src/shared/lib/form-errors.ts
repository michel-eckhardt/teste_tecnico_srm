import type { FieldValues, Path, UseFormSetError } from 'react-hook-form';

import { isApiError } from '@/shared/api/api-error';

/**
 * Shows the backend field violations (400 VALIDATION_ERROR) next to the matching form fields.
 * Returns true when every violation found a field, i.e. no generic error message is needed.
 */
export function applyServerFieldErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  fields: readonly Path<T>[],
): boolean {
  if (!isApiError(error) || error.fieldErrors.length === 0) return false;
  let allMapped = true;
  for (const violation of error.fieldErrors) {
    const field = fields.find((name) => name === violation.field);
    if (field) {
      setError(field, { type: 'server', message: violation.message });
    } else {
      allMapped = false;
    }
  }
  return allMapped;
}
