import type { MessageKey } from '../i18n/messages';

export const MAX_NIGHTS = 30;

/** Local calendar date as yyyy-MM-dd (what <input type="date"> uses). */
export function isoDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

export function addDays(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number);
  return isoDate(new Date(y, m - 1, d + days));
}

export function nightsBetween(checkIn: string, checkOut: string): number {
  const toUtc = (s: string) => {
    const [y, m, d] = s.split('-').map(Number);
    return Date.UTC(y, m - 1, d);
  };
  return Math.round((toUtc(checkOut) - toUtc(checkIn)) / 86_400_000);
}

export interface StayErrors {
  checkIn?: MessageKey;
  checkOut?: MessageKey;
}

/** Same rules as the backend's StayDates - fast feedback; the backend re-validates anyway. */
export function validateStay(checkIn: string, checkOut: string, today: string = isoDate(new Date())): StayErrors {
  const errors: StayErrors = {};
  if (!checkIn) errors.checkIn = 'validation.required';
  else if (checkIn < today) errors.checkIn = 'validation.pastDate';
  if (!checkOut) errors.checkOut = 'validation.required';
  else if (checkIn && checkOut <= checkIn) errors.checkOut = 'validation.checkOutAfter';
  else if (checkIn && nightsBetween(checkIn, checkOut) > MAX_NIGHTS) errors.checkOut = 'validation.maxNights';
  return errors;
}

export const hasErrors = (e: object) => Object.values(e).some(Boolean);
