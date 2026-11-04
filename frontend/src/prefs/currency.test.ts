import { describe, expect, it } from 'vitest';
import type { Settings } from '../sdk';
import { regionFromLocale, resolveInitialCurrency } from './currency';

const settings: Settings = {
  defaultCurrency: 'USD',
  supportedCurrencies: ['USD', 'ILS'],
  regionCurrencies: { IL: 'ILS', US: 'USD' },
  priceableCurrencies: ['USD', 'ILS'],
  updatedBy: 'system',
  updatedAt: '2026-01-01T00:00:00Z',
};

describe('display currency resolution', () => {
  it('the user choice wins while it is still offered', () => {
    expect(resolveInitialCurrency('USD', settings, 'IL')).toBe('USD');
    expect(resolveInitialCurrency('EUR', settings, 'IL')).toBe('ILS'); // no longer offered -> region
  });

  it('then the profile region, then the browser locale', () => {
    expect(resolveInitialCurrency(null, settings, 'IL')).toBe('ILS');
    expect(resolveInitialCurrency(null, settings, null, 'he-IL')).toBe('ILS');
    expect(resolveInitialCurrency(null, settings, null, 'en-US')).toBe('USD');
  });

  it('falls back to the admin default', () => {
    expect(resolveInitialCurrency(null, settings, 'FR', 'fr-FR')).toBe('USD');
    expect(resolveInitialCurrency(null, { ...settings, supportedCurrencies: ['USD'] }, 'IL')).toBe('USD');
  });

  it('parses regions from locales', () => {
    expect(regionFromLocale('he-IL')).toBe('IL');
    expect(regionFromLocale('en_us')).toBe('US');
    expect(regionFromLocale('he')).toBeNull();
  });
});
