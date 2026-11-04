import { describe, expect, it } from 'vitest';
import { addDays, nightsBetween, validateStay } from './validation';

const today = '2027-03-10';

describe('stay validation (same rules as backend StayDates)', () => {
  it('accepts a valid stay, including check-in today', () => {
    expect(validateStay(today, '2027-03-12', today)).toEqual({});
  });

  it('rejects missing, past and inverted dates', () => {
    expect(validateStay('', '', today)).toEqual({ checkIn: 'validation.required', checkOut: 'validation.required' });
    expect(validateStay('2027-03-09', '2027-03-12', today).checkIn).toBe('validation.pastDate');
    expect(validateStay('2027-03-12', '2027-03-12', today).checkOut).toBe('validation.checkOutAfter');
  });

  it('enforces the 30-night maximum', () => {
    expect(validateStay(today, addDays(today, 31), today).checkOut).toBe('validation.maxNights');
    expect(validateStay(today, addDays(today, 30), today)).toEqual({});
  });

  it('counts nights across month ends', () => {
    expect(nightsBetween('2027-02-27', '2027-03-02')).toBe(3);
  });
});
