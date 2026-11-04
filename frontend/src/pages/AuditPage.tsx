import { useState, type FormEvent } from 'react';
import { useAuth } from '../auth/AuthContext';
import { useI18n } from '../i18n/I18nProvider';
import { useAsync } from '../lib/useAsync';
import { sessionStore } from '../sdk';
import { Button, Card, ErrorAlert, Field, Input, PageHeader, Spinner } from '../ui/components';

/** /admin/audit - ops view: follow one transaction or one session end to end (audit:read). */
export function AuditPage() {
  const { sdk } = useAuth();
  const { t } = useI18n();
  const [transactionId, setTransactionId] = useState('');
  const [sessionId, setSessionId] = useState('');
  const [query, setQuery] = useState<{ transactionId?: string; sessionId?: string }>({});
  const { data, loading, error } = useAsync(() => sdk.audit.search({ ...query, limit: 100 }), [sdk, query]);

  function apply(e: FormEvent) {
    e.preventDefault();
    setQuery({ transactionId: transactionId.trim() || undefined, sessionId: sessionId.trim() || undefined });
  }

  return (
    <>
      <PageHeader title={t('audit.title')} />
      <Card className="mb-4">
        <form onSubmit={apply} className="grid gap-4 md:grid-cols-[1fr_1fr_auto] md:items-end">
          <Field label={t('audit.transaction')}>
            {(p) => <Input {...p} dir="ltr" value={transactionId} onChange={(e) => setTransactionId(e.target.value)} />}
          </Field>
          <Field label={t('audit.session')}>
            {(p) => <Input {...p} dir="ltr" value={sessionId} onChange={(e) => setSessionId(e.target.value)} />}
          </Field>
          <div className="flex flex-wrap gap-2">
            <Button type="submit">{t('audit.filter')}</Button>
            <Button type="button" variant="secondary" onClick={() => setSessionId(sessionStore.get() ?? '')}>
              {t('audit.mySession')}
            </Button>
            <Button
              type="button"
              variant="ghost"
              onClick={() => {
                setTransactionId('');
                setSessionId('');
                setQuery({});
              }}
            >
              {t('audit.latest')}
            </Button>
          </div>
        </form>
      </Card>
      {loading ? (
        <Spinner />
      ) : error ? (
        <ErrorAlert error={error} />
      ) : (
        <Card className="overflow-x-auto p-0 sm:p-0">
          <table className="w-full min-w-[720px] text-sm">
            <thead className="bg-slate-50 text-slate-500">
              <tr>
                {(['audit.when', 'audit.action', 'audit.entity', 'audit.user', 'audit.outcome', 'audit.transaction'] as const).map(
                  (k) => (
                    <th key={k} className="px-3 py-2 text-start font-medium">
                      {t(k)}
                    </th>
                  ),
                )}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {(data ?? []).map((a) => (
                <tr key={a.id} className="align-top">
                  <td className="whitespace-nowrap px-3 py-2" dir="ltr">
                    {new Date(a.timestamp).toLocaleTimeString()}
                  </td>
                  <td className="px-3 py-2 font-medium">{a.action}</td>
                  <td className="px-3 py-2" dir="ltr">
                    {a.entityType} {a.entityId}
                    {a.details && <div className="text-xs text-slate-500">{a.details}</div>}
                  </td>
                  <td className="px-3 py-2">{a.userId}</td>
                  <td className="px-3 py-2">{a.outcome ?? ''}</td>
                  <td className="px-3 py-2">
                    <button
                      type="button"
                      dir="ltr"
                      className="break-all text-start font-mono text-xs text-brand underline"
                      onClick={() => {
                        setTransactionId(a.transactionId ?? '');
                        setQuery({ transactionId: a.transactionId });
                      }}
                    >
                      {a.transactionId}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </>
  );
}
