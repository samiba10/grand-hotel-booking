import type { Http } from './http';
import type {
  AuditEntry,
  Availability,
  Booking,
  CreateBooking,
  Guest,
  LoginResponse,
  RoomType,
  UpdateBooking,
  UserProfile,
} from './types';

/** Typed client of the gateway API: pages call sdk.bookings.create(...), never fetch(). */
export function createSdk(http: Http) {
  return {
    auth: {
      login: (username: string, password: string) =>
        http.post<LoginResponse>('/api/auth/login', { username, password }),
      me: () => http.get<UserProfile>('/api/auth/me'),
    },
    rooms: {
      availability: (q: { checkIn: string; checkOut: string; roomType?: RoomType }) =>
        http.get<Availability>('/api/rooms/availability', {
          checkin: q.checkIn,
          checkout: q.checkOut,
          roomType: q.roomType,
        }),
    },
    bookings: {
      get: (id: number) => http.get<Booking>(`/api/bookings/${id}`),
      create: (body: CreateBooking) => http.post<Booking>('/api/bookings', body),
      update: (id: number, body: UpdateBooking) => http.put<Booking>(`/api/bookings/${id}`, body),
      cancel: (id: number) => http.del(`/api/bookings/${id}`),
    },
    guests: {
      get: (id: number) => http.get<Guest>(`/api/guests/${id}`),
      bookings: (id: number) => http.get<Booking[]>(`/api/guests/${id}/bookings`),
    },
    audit: {
      search: (q: { transactionId?: string; sessionId?: string; limit?: number }) =>
        http.get<AuditEntry[]>('/api/audit', q),
    },
  };
}

export type Sdk = ReturnType<typeof createSdk>;
export { createHttp } from './http';
export { ApiError, isApiError } from './errors';
export { sessionStore } from './session';
export type * from './types';
