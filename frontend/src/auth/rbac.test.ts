import { describe, expect, it } from 'vitest';
import { users } from '../test/utils';
import { can, canAny, canOn } from './rbac';

describe('FE RBAC', () => {
  it('uses only permissions returned by the backend', () => {
    expect(can(users.admin, 'audit:read')).toBe(true);
    expect(can(users.guest, 'audit:read')).toBe(false);
    expect(can(null, 'room:read')).toBe(false);
    expect(canAny(users.guest, ['audit:read', 'room:read'])).toBe(true);
  });

  it('own scope applies only to the user guest profile (mirrors backend AccessGuard)', () => {
    expect(canOn(users.guest, 'booking:update', 1)).toBe(true);
    expect(canOn(users.guest, 'booking:update', 2)).toBe(false);
  });

  it('any scope applies to every resource', () => {
    expect(canOn(users.staff, 'booking:cancel', 42)).toBe(true);
    expect(canOn(users.admin, 'booking:cancel', 42)).toBe(false); // admin fixture has no cancel permission
  });
});
