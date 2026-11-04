import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { AuthContext, type AuthState } from '../auth/AuthContext';
import { I18nProvider, type Lang } from '../i18n/I18nProvider';
import { CurrencyContext, type CurrencyState } from '../prefs/CurrencyContext';
import type { Sdk, UserProfile } from '../sdk';

/** Permissions exactly as the backend returns them (common-defaults.yml). */
export const users: Record<'guest' | 'staff' | 'admin', UserProfile> = {
  guest: {
    username: 'guest1',
    displayName: 'Dana Levi',
    roles: ['GUEST'],
    guestId: 1,
    permissions: ['room:read', 'pricing:quote', 'booking:create:own', 'booking:read:own', 'booking:update:own', 'booking:cancel:own', 'guest:read:own'],
  },
  staff: {
    username: 'staff',
    displayName: 'Front Desk',
    roles: ['STAFF'],
    permissions: ['room:read', 'pricing:quote', 'booking:create:any', 'booking:read:any', 'booking:update:any', 'booking:cancel:any', 'guest:read:any'],
  },
  admin: {
    username: 'admin',
    displayName: 'Hotel Admin',
    roles: ['ADMIN'],
    permissions: ['room:read', 'booking:read:any', 'audit:read'],
  },
};

export function fakeSdk(): Sdk {
  return {
    auth: { login: vi.fn(), me: vi.fn() },
    rooms: { availability: vi.fn() },
    bookings: { get: vi.fn(), create: vi.fn(), update: vi.fn(), cancel: vi.fn() },
    guests: { get: vi.fn(), bookings: vi.fn() },
    audit: { search: vi.fn() },
    settings: { get: vi.fn(), update: vi.fn() },
  } as unknown as Sdk;
}

export function renderWithProviders(
  ui: ReactElement,
  options: { user?: UserProfile | null; sdk?: Sdk; lang?: Lang; route?: string; currency?: string } = {},
) {
  const sdk = options.sdk ?? fakeSdk();
  const auth: AuthState = { user: options.user ?? null, ready: true, sdk, login: vi.fn(), logout: vi.fn() };
  const currency: CurrencyState = {
    currency: options.currency ?? 'USD',
    setCurrency: vi.fn(),
    settings: null,
    reloadSettings: vi.fn(),
  };
  const result = render(
    <I18nProvider defaultLang={options.lang ?? 'en'}>
      <MemoryRouter initialEntries={[options.route ?? '/']}>
        <AuthContext.Provider value={auth}>
          <CurrencyContext.Provider value={currency}>{ui}</CurrencyContext.Provider>
        </AuthContext.Provider>
      </MemoryRouter>
    </I18nProvider>,
  );
  return { ...result, sdk };
}
