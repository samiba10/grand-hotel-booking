import type { ReactNode } from 'react';
import { useAuth } from './AuthContext';
import { can, canAny, canOn } from './rbac';

type Props = {
  children: ReactNode;
  fallback?: ReactNode;
} & (
  | { permission: string; anyOf?: never; action?: never; ownerGuestId?: never }
  | { anyOf: string[]; permission?: never; action?: never; ownerGuestId?: never }
  | { action: string; ownerGuestId: number | null | undefined; permission?: never; anyOf?: never }
);

/**
 * Declarative RBAC: <Can permission="audit:read">...</Can>,
 * <Can anyOf={[...]}>, or scoped <Can action="booking:update" ownerGuestId={b.guestId}>.
 */
export function Can(props: Props) {
  const { user } = useAuth();
  let allowed: boolean;
  if (props.permission !== undefined) allowed = can(user, props.permission);
  else if (props.anyOf !== undefined) allowed = canAny(user, props.anyOf);
  else allowed = canOn(user, props.action ?? '', props.ownerGuestId);
  return <>{allowed ? props.children : (props.fallback ?? null)}</>;
}
