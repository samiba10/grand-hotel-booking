import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { can, Permission } from '../auth/rbac';
import { useI18n } from '../i18n/I18nProvider';
import { addDays, hasErrors, isoDate, validateStay, type StayErrors } from '../lib/validation';
import { useCurrency } from '../prefs/CurrencyContext';
import type { Availability, RoomOffer, RoomType } from '../sdk';
import { Alert, Badge, Button, Card, ErrorAlert, Field, Input, PageHeader, Select, Skeleton } from '../ui/components';

const ROOM_TYPES: RoomType[] = ['STANDARD', 'DELUXE', 'SUITE'];

/** /search - availability with live pricing; booking straight from the results. */
export function SearchPage() {
  const { sdk, user } = useAuth();
  const { t, money } = useI18n();
  const { currency } = useCurrency();
  const navigate = useNavigate();
  const today = isoDate(new Date());

  const [checkIn, setCheckIn] = useState(addDays(today, 7));
  const [checkOut, setCheckOut] = useState(addDays(today, 9));
  const [roomType, setRoomType] = useState<RoomType | ''>('');
  const [errors, setErrors] = useState<StayErrors>({});
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<Availability | null>(null);
  const [error, setError] = useState<unknown>(null);

  // Staff book on behalf of a guest; guests always book for themselves (decided by permissions).
  const booksForOthers = can(user, Permission.bookingCreateAny);
  const canBook = booksForOthers || can(user, Permission.bookingCreateOwn);
  const [guestId, setGuestId] = useState('');
  const [guestIdError, setGuestIdError] = useState(false);
  const [bookingRoom, setBookingRoom] = useState<number | null>(null);

  // The last valid query; changing the currency re-prices the same search.
  const [query, setQuery] = useState<{ checkIn: string; checkOut: string; roomType?: RoomType } | null>(null);

  useEffect(() => {
    if (!query) return;
    let active = true;
    setLoading(true);
    setError(null);
    sdk.rooms
      .availability({ ...query, currency })
      .then((res) => active && setResult(res))
      .catch((err: unknown) => {
        if (!active) return;
        setError(err);
        setResult(null);
      })
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [sdk, query, currency]);

  function search(e: FormEvent) {
    e.preventDefault();
    const v = validateStay(checkIn, checkOut, today);
    setErrors(v);
    if (hasErrors(v)) return;
    setQuery({ checkIn, checkOut, roomType: roomType || undefined });
  }

  async function book(room: RoomOffer) {
    if (!result) return;
    if (booksForOthers && !/^\d+$/.test(guestId)) {
      setGuestIdError(true);
      return;
    }
    setGuestIdError(false);
    setBookingRoom(room.roomId);
    setError(null);
    try {
      const booking = await sdk.bookings.create({
        roomId: room.roomId,
        checkIn: result.checkIn,
        checkOut: result.checkOut,
        guestId: booksForOthers ? Number(guestId) : undefined,
        currency: result.currency,
      });
      navigate(`/booking/${booking.id}`, { state: { justCreated: true } });
    } catch (err) {
      setError(err);
    } finally {
      setBookingRoom(null);
    }
  }

  return (
    <>
      <PageHeader title={t('search.title')} />
      <Card className="mb-6">
        <form onSubmit={search} noValidate className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 lg:items-start">
          <Field label={t('search.checkIn')} error={errors.checkIn && t(errors.checkIn)}>
            {(p) => <Input {...p} type="date" min={today} value={checkIn} onChange={(e) => setCheckIn(e.target.value)} />}
          </Field>
          <Field label={t('search.checkOut')} error={errors.checkOut && t(errors.checkOut)}>
            {(p) => (
              <Input {...p} type="date" min={checkIn || today} value={checkOut} onChange={(e) => setCheckOut(e.target.value)} />
            )}
          </Field>
          <Field label={t('search.roomType')}>
            {(p) => (
              <Select {...p} value={roomType} onChange={(e) => setRoomType(e.target.value as RoomType | '')}>
                <option value="">{t('search.anyType')}</option>
                {ROOM_TYPES.map((rt) => (
                  <option key={rt} value={rt}>
                    {t(`room.${rt}` as const)}
                  </option>
                ))}
              </Select>
            )}
          </Field>
          <div className="flex lg:pt-6">
            <Button type="submit" loading={loading} className="w-full">
              {t('search.submit')}
            </Button>
          </div>
        </form>
      </Card>

      <div className="mb-4">
        <ErrorAlert error={error} />
      </div>

      {loading && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-40" />
          ))}
        </div>
      )}

      {!loading && result && (
        <section aria-live="polite">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-lg font-semibold">{t('search.results', { count: result.rooms.length })}</h2>
            <span className="text-sm text-slate-500">{t('search.occupancy', { percent: result.occupancyPercent })}</span>
          </div>
          {result.fx?.stale && (
            <div className="mb-4">
              <Alert tone="warning">{t('search.fxStale')}</Alert>
            </div>
          )}
          {canBook && booksForOthers && result.rooms.length > 0 && (
            <div className="mb-4 max-w-xs">
              <Field label={t('search.bookFor')} error={guestIdError ? t('validation.guestId') : undefined}>
                {(p) => <Input {...p} inputMode="numeric" value={guestId} onChange={(e) => setGuestId(e.target.value)} />}
              </Field>
            </div>
          )}
          {result.rooms.length === 0 ? (
            <Alert>{t('search.none')}</Alert>
          ) : (
            <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {result.rooms.map((room) => (
                <li key={room.roomId}>
                  <Card className="flex h-full flex-col gap-3">
                    <div className="flex items-start justify-between gap-2">
                      <h3 className="text-lg font-semibold">{t('search.room', { number: room.roomNumber })}</h3>
                      <Badge tone={room.roomType === 'SUITE' ? 'gold' : 'gray'}>{t(`room.${room.roomType}` as const)}</Badge>
                    </div>
                    <div className="mt-auto">
                      <p className="text-sm text-slate-500">
                        {t('search.perNight', { amount: money(room.averageNightlyMinor, room.currency) })}
                      </p>
                      <p className="text-2xl font-semibold text-brand">
                        <span className="sr-only">{t('search.total')}: </span>
                        {money(room.totalAmountMinor, room.currency)}
                      </p>
                      <p className="text-xs text-slate-500">
                        {result.nights === 1 ? t('common.night') : t('common.nights', { count: result.nights })}
                      </p>
                    </div>
                    {canBook && (
                      <Button onClick={() => book(room)} loading={bookingRoom === room.roomId} disabled={bookingRoom !== null}>
                        {t('search.book')}
                      </Button>
                    )}
                  </Card>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </>
  );
}
