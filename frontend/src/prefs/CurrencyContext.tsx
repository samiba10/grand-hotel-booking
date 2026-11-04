import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useAuth } from '../auth/AuthContext';
import { useI18n } from '../i18n/I18nProvider';
import type { Settings } from '../sdk';
import { readStoredCurrency, resolveInitialCurrency, storeCurrency } from './currency';

export interface CurrencyState {
  currency: string;
  setCurrency: (currency: string) => void;
  /** null until loaded (or when the user is signed out). */
  settings: Settings | null;
  reloadSettings: () => void;
}

export const CurrencyContext = createContext<CurrencyState | null>(null);

/** Display currency: chosen by the user, else by region, else the system default (see currency.ts). */
export function CurrencyProvider({ children }: { children: ReactNode }) {
  const { user, sdk } = useAuth();
  const [settings, setSettings] = useState<Settings | null>(null);
  const [currency, setCurrencyState] = useState('USD');

  const reloadSettings = useCallback(() => {
    if (!user) {
      setSettings(null);
      return;
    }
    sdk.settings
      .get()
      .then(setSettings)
      .catch(() => setSettings(null));
  }, [sdk, user]);

  useEffect(reloadSettings, [reloadSettings]);

  useEffect(() => {
    if (!settings || !user) return;
    const stored = readStoredCurrency(user.username);
    setCurrencyState(resolveInitialCurrency(stored, settings, user.region, navigator.language));
  }, [settings, user]);

  const setCurrency = useCallback(
    (next: string) => {
      storeCurrency(user?.username, next);
      setCurrencyState(next);
    },
    [user],
  );

  const value = useMemo(
    () => ({ currency, setCurrency, settings, reloadSettings }),
    [currency, setCurrency, settings, reloadSettings],
  );
  return <CurrencyContext.Provider value={value}>{children}</CurrencyContext.Provider>;
}

export function useCurrency(): CurrencyState {
  const ctx = useContext(CurrencyContext);
  if (!ctx) throw new Error('useCurrency must be used inside <CurrencyProvider>');
  return ctx;
}

/** Header dropdown - shown only when the admin offers more than one currency. */
export function CurrencySelect() {
  const { currency, setCurrency, settings } = useCurrency();
  const { t } = useI18n();
  if (!settings || settings.supportedCurrencies.length < 2) return null;
  return (
    <label className="flex items-center gap-1 text-sm">
      <span className="sr-only">{t('currency.label')}</span>
      <select
        value={currency}
        onChange={(e) => setCurrency(e.target.value)}
        className="rounded-lg border border-current bg-transparent px-2 py-1.5 text-sm [&>option]:text-slate-900"
        aria-label={t('currency.label')}
      >
        {settings.supportedCurrencies.map((c) => (
          <option key={c} value={c}>
            {c === 'ILS' ? '₪ ILS' : c === 'USD' ? '$ USD' : c}
          </option>
        ))}
      </select>
    </label>
  );
}
