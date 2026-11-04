import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react';
import { useId } from 'react';
import { useI18n } from '../i18n/I18nProvider';
import type { MessageKey } from '../i18n/messages';
import { isApiError } from '../sdk';

/*
 * Style components: the only place with Tailwind class lists for shared UI. Logical utilities only
 * (ms/me, ps/pe, text-start, border-s) so every component is RTL-correct by construction.
 */

const cx = (...classes: (string | false | null | undefined)[]) => classes.filter(Boolean).join(' ');

type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost';

const buttonStyles: Record<ButtonVariant, string> = {
  primary: 'bg-brand text-white hover:bg-brand-dark',
  secondary: 'border border-slate-300 bg-white text-slate-700 hover:bg-slate-50',
  danger: 'bg-red-600 text-white hover:bg-red-700',
  ghost: 'text-brand hover:bg-brand-light',
};

export function Button({
  variant = 'primary',
  loading = false,
  className,
  children,
  disabled,
  ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: ButtonVariant; loading?: boolean }) {
  return (
    <button
      className={cx(
        'inline-flex h-10 items-center justify-center gap-2 rounded-lg px-4 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-60',
        buttonStyles[variant],
        className,
      )}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...rest}
    >
      {loading && <Spinner inline />}
      {children}
    </button>
  );
}

export function Card({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <section className={cx('rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6', className)}>
      {children}
    </section>
  );
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-2xl font-semibold text-brand sm:text-3xl">{title}</h1>
        {subtitle && <p className="mt-1 text-slate-500">{subtitle}</p>}
      </div>
      {actions}
    </div>
  );
}

/** Label + control + error, wired for accessibility (aria-invalid, aria-describedby). */
export function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: (props: { id: string; 'aria-invalid'?: boolean; 'aria-describedby'?: string }) => ReactNode;
}) {
  const id = useId();
  const errorId = `${id}-error`;
  return (
    <div className="flex flex-col gap-1 text-start">
      <label htmlFor={id} className="text-sm font-medium text-slate-700">
        {label}
      </label>
      {children({ id, 'aria-invalid': error ? true : undefined, 'aria-describedby': error ? errorId : undefined })}
      {error && (
        <p id={errorId} role="alert" className="text-sm text-red-600">
          {error}
        </p>
      )}
    </div>
  );
}

export function Input({ className, ...rest }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cx('input', rest['aria-invalid'] && 'input-invalid', className)} {...rest} />;
}

export function Select({ className, children, ...rest }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select className={cx('input', className)} {...rest}>
      {children}
    </select>
  );
}

type Tone = 'info' | 'success' | 'warning' | 'error';
const toneStyles: Record<Tone, string> = {
  info: 'border-sky-500 bg-sky-50 text-sky-900',
  success: 'border-emerald-500 bg-emerald-50 text-emerald-900',
  warning: 'border-amber-500 bg-amber-50 text-amber-900',
  error: 'border-red-500 bg-red-50 text-red-900',
};

export function Alert({
  tone = 'info',
  title,
  children,
  reference,
}: {
  tone?: Tone;
  title?: string;
  children?: ReactNode;
  reference?: string;
}) {
  const { t } = useI18n();
  return (
    <div role={tone === 'error' ? 'alert' : 'status'} className={cx('rounded-lg border-s-4 p-4 text-start', toneStyles[tone])}>
      {title && <p className="font-semibold">{title}</p>}
      {children && <div className={cx(title && 'mt-1', 'text-sm')}>{children}</div>}
      {reference && (
        <p className="mt-2 text-xs opacity-75">
          {t('common.reference')}: <code dir="ltr">{reference}</code>
        </p>
      )}
    </div>
  );
}

const statusKey: Record<number, MessageKey> = {
  0: 'error.network',
  401: 'error.unauthorized',
  404: 'error.notFound',
  429: 'error.tooMany',
  503: 'error.unavailable',
};

/**
 * BE feedback -> user feedback. 403 shows the backend's explanation (e.g. "not allowed to read
 * this resource"); 409/400 show the business message; every error shows the transaction id so
 * the user can quote it to support.
 */
export function ErrorAlert({ error }: { error: unknown }) {
  const { t } = useI18n();
  if (!error) return null;
  if (!isApiError(error)) return <Alert tone="error" title={t('error.title')} />;
  const key = statusKey[error.status];
  const title = error.status === 403 ? t('error.forbiddenTitle') : t('error.title');
  return (
    <Alert tone={error.status === 403 ? 'warning' : 'error'} title={title} reference={error.transactionId}>
      {key ? t(key) : error.message}
      {error.details.length > 0 && (
        <ul className="mt-1 list-disc ps-5">
          {error.details.map((d) => (
            <li key={d}>{d}</li>
          ))}
        </ul>
      )}
    </Alert>
  );
}

export function Spinner({ inline = false }: { inline?: boolean }) {
  const { t } = useI18n();
  const dot = <span className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />;
  if (inline) return dot;
  return (
    <div role="status" className="flex items-center justify-center gap-2 py-10 text-slate-500">
      {dot}
      <span>{t('common.loading')}</span>
    </div>
  );
}

export function Badge({ tone, children }: { tone: 'green' | 'gray' | 'gold'; children: ReactNode }) {
  const styles = {
    green: 'bg-emerald-100 text-emerald-800',
    gray: 'bg-slate-200 text-slate-700',
    gold: 'bg-gold-light text-amber-900',
  } as const;
  return <span className={cx('inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium', styles[tone])}>{children}</span>;
}

export function Skeleton({ className }: { className?: string }) {
  return <div className={cx('animate-pulse rounded-lg bg-slate-200', className)} aria-hidden="true" />;
}
