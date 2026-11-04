import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Can } from '../auth/Can';
import { Permission } from '../auth/rbac';
import { useI18n } from '../i18n/I18nProvider';
import { Badge, Button } from './components';

const link = ({ isActive }: { isActive: boolean }) =>
  `rounded-lg px-3 py-2 text-sm font-medium transition ${isActive ? 'bg-white/15 text-white' : 'text-white/80 hover:text-white'}`;

export function LanguageToggle({ className = '' }: { className?: string }) {
  const { lang, setLang, t } = useI18n();
  return (
    <button
      type="button"
      onClick={() => setLang(lang === 'he' ? 'en' : 'he')}
      className={`rounded-lg border border-current px-3 py-1.5 text-sm ${className}`}
      lang={lang === 'he' ? 'en' : 'he'}
    >
      {t('nav.language')}
    </button>
  );
}

/** Mobile-first shell: the nav wraps under the brand on small screens. */
export function Layout() {
  const { user, logout } = useAuth();
  const { t } = useI18n();
  return (
    <div className="flex min-h-screen flex-col">
      <header className="bg-brand text-white shadow">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-4 gap-y-2 px-4 py-3">
          <NavLink to="/search" className="me-auto flex items-center gap-2 text-lg font-semibold">
            <span className="grid h-8 w-8 place-items-center rounded-lg bg-gold text-brand-dark">GH</span>
            {t('app.name')}
          </NavLink>
          <nav className="order-last flex w-full flex-wrap gap-1 sm:order-none sm:w-auto">
            <Can permission={Permission.roomRead}>
              <NavLink to="/search" className={link}>
                {t('nav.search')}
              </NavLink>
            </Can>
            {user?.guestId != null && (
              <NavLink to={`/guest/${user.guestId}`} className={link}>
                {t('nav.myBookings')}
              </NavLink>
            )}
            <Can permission={Permission.auditRead}>
              <NavLink to="/admin/audit" className={link}>
                {t('nav.audit')}
              </NavLink>
            </Can>
          </nav>
          <div className="flex items-center gap-2">
            {user && (
              <span className="hidden text-sm sm:inline">
                {user.displayName}{' '}
                {user.roles.map((r) => (
                  <Badge key={r} tone="gold">
                    {t(`role.${r}` as const)}
                  </Badge>
                ))}
              </span>
            )}
            <LanguageToggle />
            <Button variant="ghost" className="text-white hover:bg-white/10" onClick={logout}>
              {t('nav.logout')}
            </Button>
          </div>
        </div>
      </header>
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 sm:py-10">
        <Outlet />
      </main>
    </div>
  );
}
