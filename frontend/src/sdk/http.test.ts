import { describe, expect, it, vi } from 'vitest';
import { ApiError } from './errors';
import { createHttp } from './http';
import { sessionStore } from './session';

function response(status: number, body: unknown, headers: Record<string, string> = {}) {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json', ...headers },
  });
}

describe('API handler (FE SDK)', () => {
  it('sends the bearer token and plays session-id ping-pong', async () => {
    const fetchFn = vi
      .fn()
      .mockResolvedValueOnce(response(200, { ok: 1 }, { 'X-Session-Id': 'sess-issued-by-gateway' }))
      .mockResolvedValueOnce(response(200, { ok: 2 }));
    const http = createHttp({ getToken: () => 'tok', fetchFn });

    await http.get('/api/a');
    await http.get('/api/b');

    const firstHeaders = fetchFn.mock.calls[0][1].headers;
    const secondHeaders = fetchFn.mock.calls[1][1].headers;
    expect(firstHeaders.Authorization).toBe('Bearer tok');
    expect(firstHeaders['X-Session-Id']).toBeUndefined();
    expect(secondHeaders['X-Session-Id']).toBe('sess-issued-by-gateway');
    expect(sessionStore.get()).toBe('sess-issued-by-gateway');
  });

  it('maps backend errors to ApiError with message, details and transaction id', async () => {
    const fetchFn = vi.fn().mockResolvedValue(
      response(409, { message: 'Room 101 is already booked', details: [], transactionId: 'BOOKING_CREATE-1' }),
    );
    const http = createHttp({ getToken: () => null, fetchFn });

    const err = await http.post('/api/bookings', {}).catch((e: unknown) => e);

    expect(err).toBeInstanceOf(ApiError);
    expect(err).toMatchObject({ status: 409, message: 'Room 101 is already booked', transactionId: 'BOOKING_CREATE-1' });
  });

  it('calls onUnauthorized on 401', async () => {
    const onUnauthorized = vi.fn();
    const http = createHttp({ getToken: () => 'expired', onUnauthorized, fetchFn: vi.fn().mockResolvedValue(response(401, {})) });
    await expect(http.get('/api/x')).rejects.toBeInstanceOf(ApiError);
    expect(onUnauthorized).toHaveBeenCalledOnce();
  });

  it('turns a network failure into status 0', async () => {
    const http = createHttp({ getToken: () => null, fetchFn: vi.fn().mockRejectedValue(new TypeError('offline')) });
    await expect(http.get('/api/x')).rejects.toMatchObject({ status: 0 });
  });

  it('returns undefined for 204', async () => {
    const http = createHttp({ getToken: () => null, fetchFn: vi.fn().mockResolvedValue(new Response(null, { status: 204 })) });
    await expect(http.del('/api/bookings/1')).resolves.toBeUndefined();
  });

  it('drops empty query parameters', async () => {
    const fetchFn = vi.fn().mockResolvedValue(response(200, {}));
    const http = createHttp({ getToken: () => null, fetchFn });
    await http.get('/api/rooms/availability', { checkin: '2027-01-01', roomType: undefined });
    expect(fetchFn.mock.calls[0][0]).toMatch(/checkin=2027-01-01$/);
  });
});
