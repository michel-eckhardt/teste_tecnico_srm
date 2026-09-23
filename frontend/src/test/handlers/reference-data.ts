import { http, HttpResponse } from 'msw';

import { apiUrl } from '../api';
import { currencies, receivableTypes } from '../fixtures';

export const referenceDataHandlers = [
  http.get(apiUrl('/currencies'), () => HttpResponse.json(currencies)),
  http.get(apiUrl('/receivable-types'), () => HttpResponse.json(receivableTypes)),
];
