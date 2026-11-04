import { Navigate, Route, Routes } from 'react-router-dom';
import { RequireAuth, RequirePermission } from './auth/guards';
import { Permission } from './auth/rbac';
import { AdminSettingsPage } from './pages/AdminSettingsPage';
import { AuditPage } from './pages/AuditPage';
import { BookingPage } from './pages/BookingPage';
import { GuestPage } from './pages/GuestPage';
import { LoginPage } from './pages/LoginPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { SearchPage } from './pages/SearchPage';
import { Layout } from './ui/Layout';

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route index element={<Navigate to="/search" replace />} />
        <Route
          path="/search"
          element={
            <RequirePermission anyOf={[Permission.roomRead]}>
              <SearchPage />
            </RequirePermission>
          }
        />
        <Route path="/booking/:id" element={<BookingPage />} />
        <Route path="/guest/:id" element={<GuestPage />} />
        <Route
          path="/admin/audit"
          element={
            <RequirePermission anyOf={[Permission.auditRead]}>
              <AuditPage />
            </RequirePermission>
          }
        />
        <Route
          path="/admin/settings"
          element={
            <RequirePermission anyOf={[Permission.settingsWrite]}>
              <AdminSettingsPage />
            </RequirePermission>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
