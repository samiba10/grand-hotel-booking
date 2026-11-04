import { useState, type FormEvent, type ReactNode } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Can } from '../auth/Can';
import { canOn, Permission } from '../auth/rbac';
import { useI18n } from '../i18n/I18nProvider';
import { useAsync } from '../lib/useAsync';
import { hasErrors, isoDate, validateStay, type StayErrors } from '../lib/validation';
import { Alert, Badge, Button, Card, ErrorAlert, Field, Input, PageHeader, Spinner } from '../ui/components';

/** /booking/:id - view, change dates, cancel. Controls follow the user's permissions on THIS booking. */
export function BookingPage() {
  const id = Number(useParams().id);
  const { sdk, user } = useAuth();
  const { t, money, date } = useI18n();
  const justCreated = (useLocation().state as { justCreated?: boolean } | null)?.justCreated;
  const { data: booking, loading, error, setData } = useAsync(() => sdk.bookings.get(id), [sdk, id]);

  const [editing, setEditing] = useState(false);
  const [checkIn, setCheckIn] = useState('');
  const [checkOut, setCheckOut] = useState('');
  const [errors, setErrors] = useState<StayErrors>({});
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<unknown>(null);
  const [notice, setNotice] = useState<string | null>(justCreated ? t('booking.created.ok') : null);
  const [confirmCancel, setConfirmCancel] = useState(false);

  if (loading) return <Spinner />;
  if (error || !booking) return <ErrorAlert error={error} />;

  const active = booking.status === 'CONFIRMED';
  const mayEdit = active && canOn(user, Permission.booking.update, booking.guestId);
  const mayCancel = active && canOn(user, Permission.booking.cancel, booking.guestId);

  function startEdit() {
    if (!booking) return;
    setCheckIn(booking.checkIn);
    setCheckOut(booking.checkOut);
    setErrors({});
    setEditing(true);
  }

  async function save(e: FormEvent) {
    e.preventDefault();
    if (!booking) return;
    const v = validateStay(checkIn, checkOut, isoDate(new Date()));
    setErrors(v);
    if (hasErrors(v)) return;
    setBusy(true);
    setActionError(null);
    try {
      setData(await sdk.bookings.update(booking.id, { roomId: booking.roomId, checkIn, checkOut }));
      setEditing(false);
      setNotice(t('booking.saved'));
    } catch (err) {
      setActionError(err);
    } finally {
      setBusy(false);
    }
  }

  async function cancel() {
    if (!booking) return;
    setBusy(true);
    setActionError(null);
    try {
      await sdk.bookings.cancel(booking.id);
      setData(await sdk.bookings.get(booking.id));
      setConfirmCancel(false);
      setNotice(t('booking.cancelled'));
    } catch (err) {
      setActionError(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <PageHeader
        title={t('booking.title', { id: booking.id })}
        actions={
          <Can action={Permission.booking.read} ownerGuestId={booking.guestId}>
            <Link to={`/guest/${booking.guestId}`} className="text-sm font-medium text-brand underline">
              {t('booking.guestHistory')}
            </Link>
          </Can>
        }
      />
      <div className="flex flex-col gap-4">
        {notice && <Alert tone="success">{notice}</Alert>}
        <ErrorAlert error={actionError} />

        <Card>
          <dl className="grid gap-4 sm:grid-cols-2">
            <Item label={t('booking.guest')}>
              {booking.guestName} <span className="text-slate-400">#{booking.guestId}</span>
            </Item>
            <Item label={t('booking.room')}>
              {booking.roomNumber} · {t(`room.${booking.roomType}` as const)}
            </Item>
            <Item label={t('booking.dates')}>
              {date(booking.checkIn)} → {date(booking.checkOut)} ({t('common.nights', { count: booking.nights })})
            </Item>
            <Item label={t('booking.total')}>
              <span className="text-xl font-semibold text-brand">{money(booking.totalAmountMinor, booking.currency)}</span>
            </Item>
            <Item label={t('booking.created')}>{date(booking.createdAt)}</Item>
            <Item label={t('booking.status')}>
              <Badge tone={active ? 'green' : 'gray'}>{t(`status.${booking.status}` as const)}</Badge>
            </Item>
          </dl>
        </Card>

        {active && !mayEdit && !mayCancel && <Alert>{t('booking.readOnly')}</Alert>}

        {editing ? (
          <Card>
            <form onSubmit={save} noValidate className="grid gap-4 sm:grid-cols-3 sm:items-start">
              <Field label={t('search.checkIn')} error={errors.checkIn && t(errors.checkIn)}>
                {(p) => <Input {...p} type="date" value={checkIn} onChange={(e) => setCheckIn(e.target.value)} />}
              </Field>
              <Field label={t('search.checkOut')} error={errors.checkOut && t(errors.checkOut)}>
                {(p) => <Input {...p} type="date" value={checkOut} onChange={(e) => setCheckOut(e.target.value)} />}
              </Field>
              <div className="flex gap-2 sm:pt-6">
                <Button type="submit" loading={busy}>
                  {t('common.save')}
                </Button>
                <Button type="button" variant="secondary" onClick={() => setEditing(false)}>
                  {t('common.cancel')}
                </Button>
              </div>
            </form>
          </Card>
        ) : (
          <div className="flex flex-wrap gap-2">
            {mayEdit && (
              <Button variant="secondary" onClick={startEdit}>
                {t('booking.edit')}
              </Button>
            )}
            {mayCancel && !confirmCancel && (
              <Button variant="danger" onClick={() => setConfirmCancel(true)}>
                {t('booking.cancel')}
              </Button>
            )}
          </div>
        )}

        {confirmCancel && (
          <Alert tone="warning" title={t('booking.confirmCancel')}>
            <div className="mt-2 flex gap-2">
              <Button variant="danger" loading={busy} onClick={cancel}>
                {t('booking.confirmYes')}
              </Button>
              <Button variant="secondary" onClick={() => setConfirmCancel(false)}>
                {t('common.back')}
              </Button>
            </div>
          </Alert>
        )}
      </div>
    </>
  );
}

function Item({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="text-start">
      <dt className="text-sm text-slate-500">{label}</dt>
      <dd className="mt-0.5 font-medium">{children}</dd>
    </div>
  );
}
