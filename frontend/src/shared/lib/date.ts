import dayjs from 'dayjs';

import type { IsoDate, IsoDateTime } from '@/shared/api/contract';

/**
 * Date helpers. Calendar dates (`IsoDate`) are handled as plain strings, so no time zone shift can
 * turn a due date into the day before; instants (`IsoDateTime`) are displayed in the browser zone.
 */

const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})$/;
const BR_DATE = /^(\d{2})\/(\d{2})\/(\d{4})$/;

const dateTimeFormatter = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short',
  timeStyle: 'short',
});

export function isIsoDate(value: string): boolean {
  const match = ISO_DATE.exec(value);
  if (!match) return false;
  const [, year, month, day] = match;
  return isRealDate(Number(year), Number(month), Number(day));
}

/** `"2026-12-22"` → `"22/12/2026"` (no Date object involved). */
export function formatDate(value: IsoDate): string {
  const match = ISO_DATE.exec(value);
  if (!match) return value;
  const [, year, month, day] = match;
  return `${day ?? ''}/${month ?? ''}/${year ?? ''}`;
}

/** `"23/09/2026"` → `"2026-09-23"`; `null` for anything that is not a real calendar date. */
export function parseBrDate(value: string): IsoDate | null {
  const match = BR_DATE.exec(value.trim());
  if (!match) return null;
  const [, day, month, year] = match;
  if (!isRealDate(Number(year), Number(month), Number(day))) return null;
  return `${year ?? ''}-${month ?? ''}-${day ?? ''}`;
}

/** Instant in the operator's time zone: `"2026-09-23T14:05:00Z"` → `"23/09/2026, 11:05"`. */
export function formatDateTime(value: IsoDateTime): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : dateTimeFormatter.format(date);
}

export function today(now: Date = new Date()): IsoDate {
  return dayjs(now).format('YYYY-MM-DD');
}

/** First valid due date: the backend requires a due date after the operation date. */
export function tomorrow(now: Date = new Date()): IsoDate {
  return dayjs(now).add(1, 'day').format('YYYY-MM-DD');
}

/** Calendar arithmetic on ISO dates: `addDays("2026-12-31", 1)` → `"2027-01-01"`. */
export function addDays(date: IsoDate, days: number): IsoDate {
  const time = Date.parse(`${date}T00:00:00Z`) + days * 86_400_000;
  return new Date(time).toISOString().slice(0, 10);
}

/** Whole days from `from` to `to` (calendar dates, DST-proof). */
export function daysBetween(from: IsoDate, to: IsoDate): number {
  return Math.round((Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`)) / 86_400_000);
}

function isRealDate(year: number, month: number, day: number): boolean {
  if (month < 1 || month > 12 || day < 1) return false;
  const daysInMonth = new Date(Date.UTC(year, month, 0)).getUTCDate();
  return day <= daysInMonth;
}
