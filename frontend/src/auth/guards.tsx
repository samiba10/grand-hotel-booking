import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useI18n } from '../i18n/I18nProvider';
import { Alert, Spinner } from '../ui/components';
import { useAuth } from './AuthContext';
import { canAny } from './rbac';

export function RequireAuth({ children }: { children: ReactNode }) {
  const { user, ready } = useAuth();
  const location = useLocation();
  if (!ready) return <Spinner />;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return <>{children}</>;
}

/** Route-level RBAC: explains WHY the page is not available instead of a blank screen. */
export function RequirePermission({ anyOf, children }: { anyOf: string[]; children: ReactNode }) {
  const { user } = useAuth();
  const { t } = useI18n();
  if (!canAny(user, anyOf)) {
    return (
      <Alert tone="warning" title={t('error.forbiddenTitle')}>
        {t('error.forbiddenPage')}
      </Alert>
    );
  }
  return <>{children}</>;
}
