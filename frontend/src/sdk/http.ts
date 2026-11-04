import { ApiError } from './errors';
import { sessionStore } from './session';
import type { ApiErrorBody } from './types';

export type Query = Record<string, string | number | undefined>;

export interface HttpOptions {
  baseUrl?: string;
  getToken: () => string | null;
  /** Called on any 401 - the app logs out and redirects to login. */
  onUnauthorized?: () => void;
  fetchFn?: typeof fetch;
}

/**
 * The API handler: the ONLY place that talks HTTP. Adds the bearer token and the session id,
 * captures the session id the gateway returns, and turns every failure into an ApiError carrying
 * the backend's message and transaction id.
 */
export function createHttp(options: HttpOptions) {
  const fetchFn: typeof fetch = options.fetchFn ?? ((input, init) => fetch(input, init));

  async function request<T>(method: string, path: string, query?: Query, body?: unknown): Promise<T> {
    const url = new URL((options.baseUrl ?? '') + path, window.location.origin);
    for (const [key, value] of Object.entries(query ?? {})) {
      if (value !== undefined && value !== '') url.searchParams.set(key, String(value));
    }

    const headers: Record<string, string> = { Accept: 'application/json' };
    const token = options.getToken();
    if (token) headers.Authorization = `Bearer ${token}`;
    const sessionId = sessionStore.get();
    if (sessionId) headers['X-Session-Id'] = sessionId;
    if (body !== undefined) headers['Content-Type'] = 'application/json';

    let res: Response;
    try {
      res = await fetchFn(url.toString(), {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch {
      throw new ApiError(0, 'Network error');
    }

    const returnedSession = res.headers.get('X-Session-Id');
    if (returnedSession) sessionStore.set(returnedSession);
    const transactionId = res.headers.get('X-Transaction-Id') ?? undefined;

    const text = res.status === 204 ? '' : await res.text();
    const data: unknown = text ? parseJson(text) : undefined;

    if (!res.ok) {
      const err = (data ?? {}) as Partial<ApiErrorBody>;
      if (res.status === 401) options.onUnauthorized?.();
      throw new ApiError(
        res.status,
        err.message ?? res.statusText,
        err.details ?? [],
        err.transactionId ?? transactionId,
      );
    }
    return data as T;
  }

  return {
    get: <T>(path: string, query?: Query) => request<T>('GET', path, query),
    post: <T>(path: string, body: unknown) => request<T>('POST', path, undefined, body),
    put: <T>(path: string, body: unknown) => request<T>('PUT', path, undefined, body),
    del: (path: string) => request<void>('DELETE', path),
  };
}

export type Http = ReturnType<typeof createHttp>;

function parseJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return undefined;
  }
}
