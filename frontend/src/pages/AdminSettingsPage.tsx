import { useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../auth/AuthContext';
import { useI18n } from '../i18n/I18nProvider';
import { useAsync } from '../lib/useAsync';
import { useCurrency } from '../prefs/CurrencyContext';
import { Alert, Button, Card, ErrorAlert, Field, Input, PageHeader, Select, Spinner } from '../ui/components';

type RegionRow = { region: string; currency: string };

/** /admin/settings - runtime system configuration (settings:write). Every save is audited. */
export function AdminSettingsPage() {
  const { sdk } = useAuth();
  const { t, date } = useI18n();
  const { reloadSettings } = useCurrency();
  const { data, loading, error, setData } = useAsync(() => sdk.settings.get(), [sdk]);

  const [supported, setSupported] = useState<string[]>([]);
  const [defaultCurrency, setDefaultCurrency] = useState('');
  const [regions, setRegions] = useState<RegionRow[]>([]);
  const [busy, setBusy] = useState(false);
  const [saveError, setSaveError] = useState<unknown>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (!data) return;
    setSupported(data.supportedCurrencies);
    setDefaultCurrency(data.defaultCurrency);
    setRegions(Object.entries(data.regionCurrencies).map(([region, currency]) => ({ region, currency })));
  }, [data]);

  if (loading) return <Spinner />;
  if (error || !data) return <ErrorAlert error={error} />;

  const invalidRegion = regions.some((r) => !/^[A-Z]{2}$/.test(r.region));

  function toggle(c: string) {
    setSupported((s) => (s.includes(c) ? s.filter((x) => x !== c) : [...s, c]));
    setSaved(false);
  }

  async function save(e: FormEvent) {
    e.preventDefault();
    if (invalidRegion) return;
    setBusy(true);
    setSaveError(null);
    setSaved(false);
    try {
      const res = await sdk.settings.update({
        defaultCurrency,
        supportedCurrencies: supported,
        regionCurrencies: Object.fromEntries(regions.map((r) => [r.region, r.currency])),
      });
      setData(res);
      setSaved(true);
      reloadSettings();
    } catch (err) {
      setSaveError(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <PageHeader
        title={t('settings.title')}
        subtitle={t('settings.updatedBy', { user: data.updatedBy, when: date(data.updatedAt) })}
      />
      <form onSubmit={save} noValidate className="flex max-w-2xl flex-col gap-4">
        {saved && <Alert tone="success">{t('settings.saved')}</Alert>}
        {/* Backend validation (e.g. default not offered) comes back as 400 with its message. */}
        <ErrorAlert error={saveError} />

        <Card>
          <h2 className="mb-3 font-semibold">{t('settings.currencies')}</h2>
          <fieldset className="mb-4">
            <legend className="mb-2 text-sm font-medium text-slate-700">{t('settings.supported')}</legend>
            <div className="flex flex-wrap gap-4">
              {data.priceableCurrencies.map((c) => (
                <label key={c} className="flex items-center gap-2">
                  <input type="checkbox" checked={supported.includes(c)} onChange={() => toggle(c)} />
                  <span dir="ltr">{c}</span>
                </label>
              ))}
            </div>
          </fieldset>
          <Field label={t('settings.default')}>
            {(p) => (
              <Select {...p} value={defaultCurrency} onChange={(e) => setDefaultCurrency(e.target.value)}>
                {supported.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </Select>
            )}
          </Field>
        </Card>

        <Card>
          <h2 className="font-semibold">{t('settings.regions')}</h2>
          <p className="mb-3 text-sm text-slate-500">{t('settings.regionHint')}</p>
          <ul className="flex flex-col gap-2">
            {regions.map((row, i) => (
              <li key={i} className="grid grid-cols-[1fr_1fr_auto] items-end gap-2">
                <Field label={t('settings.region')} error={/^[A-Z]{2}$/.test(row.region) ? undefined : t('settings.invalidRegion')}>
                  {(p) => (
                    <Input
                      {...p}
                      dir="ltr"
                      maxLength={2}
                      value={row.region}
                      onChange={(e) =>
                        setRegions((rs) => rs.map((r, j) => (j === i ? { ...r, region: e.target.value.toUpperCase() } : r)))
                      }
                    />
                  )}
                </Field>
                <Field label={t('currency.label')}>
                  {(p) => (
                    <Select
                      {...p}
                      value={row.currency}
                      onChange={(e) =>
                        setRegions((rs) => rs.map((r, j) => (j === i ? { ...r, currency: e.target.value } : r)))
                      }
                    >
                      {supported.map((c) => (
                        <option key={c} value={c}>
                          {c}
                        </option>
                      ))}
                    </Select>
                  )}
                </Field>
                <Button type="button" variant="ghost" onClick={() => setRegions((rs) => rs.filter((_, j) => j !== i))}>
                  {t('settings.remove')}
                </Button>
              </li>
            ))}
          </ul>
          <Button
            type="button"
            variant="secondary"
            className="mt-3"
            onClick={() => setRegions((rs) => [...rs, { region: '', currency: defaultCurrency }])}
          >
            {t('settings.addRegion')}
          </Button>
        </Card>

        <div>
          <Button type="submit" loading={busy} disabled={invalidRegion || supported.length === 0}>
            {t('common.save')}
          </Button>
        </div>
      </form>
    </>
  );
}
