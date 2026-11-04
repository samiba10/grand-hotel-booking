import type { UserProfile } from '../sdk';

/**
 * FE RBAC mirrors the backend vocabulary (resource:action[:scope]) and uses ONLY the permissions
 * the backend returned for this user. It decides what to SHOW; the backend still decides what is
 * ALLOWED - a hidden button is UX, not security.
 */
export function can(user: UserProfile | null, permission: string): boolean {
  return !!user && user.permissions.includes(permission);
}

export function canAny(user: UserProfile | null, permissions: string[]): boolean {
  return permissions.some((p) => can(user, p));
}

/** Same rule as the backend AccessGuard: ":any", or ":own" on a resource of the user's guest. */
export function canOn(user: UserProfile | null, action: string, ownerGuestId?: number | null): boolean {
  if (can(user, `${action}:any`)) return true;
  return can(user, `${action}:own`) && user?.guestId != null && user.guestId === ownerGuestId;
}

export const Permission = {
  roomRead: 'room:read',
  auditRead: 'audit:read',
  bookingCreateAny: 'booking:create:any',
  bookingCreateOwn: 'booking:create:own',
  booking: { read: 'booking:read', update: 'booking:update', cancel: 'booking:cancel' },
  guestRead: 'guest:read',
} as const;
