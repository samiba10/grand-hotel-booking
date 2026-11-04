/** Wire types of the backend API (see docs/api.md). Money is always integer minor units. */

export type Role = 'GUEST' | 'STAFF' | 'ADMIN';
export type RoomType = 'STANDARD' | 'DELUXE' | 'SUITE';
export type BookingStatus = 'CONFIRMED' | 'CANCELLED';

export interface UserProfile {
  username: string;
  displayName: string;
  roles: Role[];
  /** Resolved by the backend from the roles - the ONLY input of the UI's RBAC layer. */
  permissions: string[];
  /** Guest profile owned by this account; absent for staff. */
  guestId?: number;
  /** ISO-3166 alpha-2, e.g. IL - picks the default display currency. */
  region?: string;
}

export interface LoginResponse {
  token: string;
  expiresAt: string;
  user: UserProfile;
}

export interface RoomOffer {
  roomId: number;
  roomNumber: string;
  roomType: RoomType;
  totalAmountMinor: number;
  averageNightlyMinor: number;
  currency: string;
}

export interface Availability {
  hotelId: number;
  hotelName: string;
  checkIn: string;
  checkOut: string;
  nights: number;
  occupancyPercent: number;
  currency: string;
  fx?: { rateMicros: number; asOf: string; stale: boolean };
  rooms: RoomOffer[];
}

export interface Booking {
  id: number;
  guestId: number;
  guestName: string;
  roomId: number;
  roomNumber: string;
  roomType: RoomType;
  checkIn: string;
  checkOut: string;
  nights: number;
  status: BookingStatus;
  totalAmountMinor: number;
  currency: string;
  fxRateMicros: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBooking {
  roomId: number;
  checkIn: string;
  checkOut: string;
  guestId?: number;
  currency?: string;
}

/** System settings (admin-owned). The server is the source of truth for offered currencies. */
export interface Settings {
  defaultCurrency: string;
  supportedCurrencies: string[];
  regionCurrencies: Record<string, string>;
  /** What the pricing chain can quote - the admin may offer any subset. */
  priceableCurrencies: string[];
  updatedBy: string;
  updatedAt: string;
}

export interface UpdateSettings {
  defaultCurrency: string;
  supportedCurrencies: string[];
  regionCurrencies: Record<string, string>;
}

export interface UpdateBooking {
  roomId: number;
  checkIn: string;
  checkOut: string;
}

export interface Guest {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
}

export interface AuditEntry {
  id: number;
  action: string;
  entityType: string;
  entityId: string;
  userId: string;
  timestamp: string;
  transactionId?: string;
  transactionType?: string;
  sessionId?: string;
  outcome?: number;
  details?: string;
}

/** The backend's single error contract. */
export interface ApiErrorBody {
  status: number;
  error: string;
  message: string;
  path: string;
  details: string[];
  transactionId?: string;
}
