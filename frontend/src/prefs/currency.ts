import type { Settings } from '../sdk';

/** "he-IL" -> "IL"; no region part -> null. */
export function regionFromLocale(locale: string | undefined): string | null {
  const match = /[-_]([A-Za-z]{2})\b/.exec(locale ?? '');
  return match ? match[1].toUpperCase() : null;
}

/**
 * Which currency to show, in priority order:
 * 1. the user's own choice (if still offered), 2. their region per the admin's mapping
 * (profile region, else browser locale), 3. the system default.
 */
export function resolveInitialCurrency(
  stored: string | null,
  settings: Settings,
  region: string | null | undefined,
  browserLocale?: string,
): string {
  const offered = settings.supportedCurrencies;
  if (stored && offered.includes(stored)) return stored;
  const effectiveRegion = region || regionFromLocale(browserLocale);
  const byRegion = effectiveRegion ? settings.regionCurrencies[effectiveRegion] : undefined;
  if (byRegion && offered.includes(byRegion)) return byRegion;
  return settings.defaultCurrency;
}

const key = (username: string) => `gh.currency.${username}`;

export function readStoredCurrency(username: string | undefined): string | null {
  if (!username) return null;
  try {
    return localStorage.getItem(key(username));
  } catch {
    return null;
  }
}

export function storeCurrency(username: string | undefined, currency: string): void {
  if (!username) return;
  try {
    localStorage.setItem(key(username), currency);
  } catch {
    // preference kept for this page view only
  }
}
