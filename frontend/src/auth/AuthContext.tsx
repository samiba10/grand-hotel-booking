import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { createHttp, createSdk, type Sdk, type UserProfile } from '../sdk';

const TOKEN_KEY = 'gh.token';

export interface AuthState {
  user: UserProfile | null;
  /** false while the stored token is being validated against /api/auth/me */
  ready: boolean;
  sdk: Sdk;
  login: (username: string, password: string) => Promise<UserProfile>;
  logout: () => void;
}

export const AuthContext = createContext<AuthState | null>(null);

function readToken(): string | null {
  try {
    return sessionStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

function writeToken(token: string | null): void {
  try {
    if (token) sessionStorage.setItem(TOKEN_KEY, token);
    else sessionStorage.removeItem(TOKEN_KEY);
  } catch {
    // storage unavailable: the token lives in memory for this tab only
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const tokenRef = useRef<string | null>(readToken());
  const [user, setUser] = useState<UserProfile | null>(null);
  const [ready, setReady] = useState(tokenRef.current === null);

  const logout = useCallback(() => {
    tokenRef.current = null;
    writeToken(null);
    setUser(null);
  }, []);

  const sdk = useMemo(
    () => createSdk(createHttp({ getToken: () => tokenRef.current, onUnauthorized: logout })),
    [logout],
  );

  // A stored token is not trusted blindly: the backend confirms identity AND permissions.
  useEffect(() => {
    if (!tokenRef.current) return;
    sdk.auth
      .me()
      .then(setUser)
      .catch(logout)
      .finally(() => setReady(true));
  }, [sdk, logout]);

  const login = useCallback(
    async (username: string, password: string) => {
      const res = await sdk.auth.login(username, password);
      tokenRef.current = res.token;
      writeToken(res.token);
      setUser(res.user);
      return res.user;
    },
    [sdk],
  );

  const value = useMemo(() => ({ user, ready, sdk, login, logout }), [user, ready, sdk, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
