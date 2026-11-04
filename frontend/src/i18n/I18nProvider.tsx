import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { en, he, type MessageKey } from './messages';

export type Lang = 'en' | 'he';

interface I18nState {
  lang: Lang;
  dir: 'ltr' | 'rtl';
  setLang: (lang: Lang) => void;
  t: (key: MessageKey, vars?: Record<string, string | number>) => string;
  /** Integer minor units -> localized currency. Display only (division is exact for /100). */
  money: (amountMinor: number, currency: string) => string;
  date: (iso: string) => string;
}

const dictionaries = { en, he } as const;
const LANG_KEY = 'gh.lang';

const I18nContext = createContext<I18nState | null>(null);

function initialLang(): Lang {
  try {
    return localStorage.getItem(LANG_KEY) === 'he' ? 'he' : 'en';
  } catch {
    return 'en';
  }
}

export function I18nProvider({ children, defaultLang }: { children: ReactNode; defaultLang?: Lang }) {
  const [lang, setLangState] = useState<Lang>(defaultLang ?? initialLang());
  const dir = lang === 'he' ? 'rtl' : 'ltr';

  // dir on <html>: the browser, Tailwind's rtl: variant and logical utilities all follow it.
  useEffect(() => {
    document.documentElement.lang = lang;
    document.documentElement.dir = dir;
  }, [lang, dir]);

  const setLang = useCallback((next: Lang) => {
    setLangState(next);
    try {
      localStorage.setItem(LANG_KEY, next);
    } catch {
      // preference not persisted
    }
  }, []);

  const value = useMemo<I18nState>(() => {
    const locale = lang === 'he' ? 'he-IL' : 'en-US';
    const dict = dictionaries[lang];
    return {
      lang,
      dir,
      setLang,
      t: (key, vars) =>
        Object.entries(vars ?? {}).reduce<string>(
          (text, [name, val]) => text.replaceAll(`{${name}}`, String(val)),
          dict[key] ?? en[key],
        ),
      money: (amountMinor, currency) =>
        new Intl.NumberFormat(locale, { style: 'currency', currency }).format(amountMinor / 100),
      date: (iso) =>
        new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeZone: 'UTC' }).format(new Date(iso)),
    };
  }, [lang, dir, setLang]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18nState {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error('useI18n must be used inside <I18nProvider>');
  return ctx;
}
