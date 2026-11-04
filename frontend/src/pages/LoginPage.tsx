import { useState, type FormEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useI18n } from '../i18n/I18nProvider';
import { isApiError } from '../sdk';
import { Alert, Button, Card, ErrorAlert, Field, Input } from '../ui/components';
import { LanguageToggle } from '../ui/Layout';

const DEMO = [
  { username: 'guest1', password: 'Guest123!', label: 'guest1 - Guest (Dana)' },
  { username: 'guest2', password: 'Guest123!', label: 'guest2 - Guest (John)' },
  { username: 'staff', password: 'Staff123!', label: 'staff - Staff' },
  { username: 'admin', password: 'Admin123!', label: 'admin - Admin' },
];

export function LoginPage() {
  const { login } = useAuth();
  const { t } = useI18n();
  const navigate = useNavigate();
  const from = (useLocation().state as { from?: string } | null)?.from;
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [touched, setTouched] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setTouched(true);
    if (!username || !password) return;
    setBusy(true);
    setError(null);
    try {
      await login(username, password);
      navigate(from ?? '/search', { replace: true });
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-b from-brand to-brand-dark px-4 py-10">
      <div className="w-full max-w-md">
        <div className="mb-6 flex items-center justify-between text-white">
          <div>
            <h1 className="text-3xl font-semibold">{t('app.name')}</h1>
            <p className="text-white/70">{t('app.tagline')}</p>
          </div>
          <LanguageToggle />
        </div>
        <Card>
          <h2 className="mb-4 text-xl font-semibold text-brand">{t('login.title')}</h2>
          <form onSubmit={submit} noValidate className="flex flex-col gap-4">
            <Field label={t('login.username')} error={touched && !username ? t('validation.required') : undefined}>
              {(p) => <Input {...p} autoComplete="username" value={username} onChange={(e) => setUsername(e.target.value)} />}
            </Field>
            <Field label={t('login.password')} error={touched && !password ? t('validation.required') : undefined}>
              {(p) => (
                <Input
                  {...p}
                  type="password"
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              )}
            </Field>
            {isApiError(error) && error.status === 401 ? (
              <Alert tone="error" reference={error.transactionId}>
                {t('login.invalid')}
              </Alert>
            ) : (
              <ErrorAlert error={error} />
            )}
            <Button type="submit" loading={busy}>
              {t('login.submit')}
            </Button>
          </form>
          <div className="mt-6 border-t border-slate-200 pt-4">
            <p className="mb-2 text-sm font-medium text-slate-600">{t('login.demo')}</p>
            <div className="grid gap-2 sm:grid-cols-2">
              {DEMO.map((d) => (
                <Button
                  key={d.username}
                  type="button"
                  variant="secondary"
                  className="justify-start text-xs"
                  onClick={() => {
                    setUsername(d.username);
                    setPassword(d.password);
                  }}
                >
                  <span dir="ltr">{d.label}</span>
                </Button>
              ))}
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
