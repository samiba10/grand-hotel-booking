import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useI18n } from '../i18n/I18nProvider';
import { useAsync } from '../lib/useAsync';
import { isoDate } from '../lib/validation';
import { Badge, Card, ErrorAlert, PageHeader, Spinner } from '../ui/components';

/**
 * /guest/:id - profile + booking history. A guest opening another guest's id gets the backend's
 * 403 explained on screen - the UI never decides ownership on its own.
 */
export function GuestPage() {
  const id = Number(useParams().id);
  const { sdk } = useAuth();
  const { t, money, date } = useI18n();
  const { data, loading, error } = useAsync(
    () => Promise.all([sdk.guests.get(id), sdk.guests.bookings(id)]),
    [sdk, id],
  );

  if (loading) return <Spinner />;
  if (error || !data) return <ErrorAlert error={error} />;
  const [guest, bookings] = data;
  const today = isoDate(new Date());
  const upcoming = bookings.filter((b) => b.status === 'CONFIRMED' && b.checkOut >= today);

  return (
    <>
      <PageHeader title={`${guest.firstName} ${guest.lastName}`} subtitle={t('guest.title')} />
      <div className="grid gap-4 lg:grid-cols-3">
        <Card>
          <h2 className="mb-3 font-semibold">{t('guest.contact')}</h2>
          <p dir="ltr" className="text-start">
            {guest.email}
          </p>
          {guest.phone && (
            <p dir="ltr" className="text-start text-slate-500">
              {guest.phone}
            </p>
          )}
          <p className="mt-4 text-sm text-slate-500">
            {t('guest.upcoming')}: <span className="font-semibold text-brand">{upcoming.length}</span>
          </p>
        </Card>

        <Card className="lg:col-span-2">
          <h2 className="mb-3 font-semibold">{t('guest.history')}</h2>
          {bookings.length === 0 ? (
            <p className="text-slate-500">
              {t('guest.none')}{' '}
              <Link to="/search" className="text-brand underline">
                {t('guest.findRoom')}
              </Link>
            </p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {bookings.map((b) => (
                <li key={b.id}>
                  <Link
                    to={`/booking/${b.id}`}
                    className="flex flex-wrap items-center justify-between gap-2 py-3 hover:bg-slate-50 sm:px-2"
                  >
                    <div className="text-start">
                      <p className="font-medium">
                        #{b.id} · {t('search.room', { number: b.roomNumber })} · {t(`room.${b.roomType}` as const)}
                      </p>
                      <p className="text-sm text-slate-500">
                        {date(b.checkIn)} → {date(b.checkOut)}
                      </p>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="font-semibold">{money(b.totalAmountMinor, b.currency)}</span>
                      <Badge tone={b.status === 'CONFIRMED' ? 'green' : 'gray'}>{t(`status.${b.status}` as const)}</Badge>
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </>
  );
}
