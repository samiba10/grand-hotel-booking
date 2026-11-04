/**
 * Session id "ping-pong": the gateway issues X-Session-Id, the SDK stores it and echoes it on every
 * request, so ops can follow this browser's whole journey (before and after login) in the logs and
 * the audit trail. Storage can be unavailable (private mode) - memory is the fallback.
 */
const KEY = 'gh.sessionId';
let memory: string | null = null;

export const sessionStore = {
  get(): string | null {
    try {
      return localStorage.getItem(KEY) ?? memory;
    } catch {
      return memory;
    }
  },
  set(id: string): void {
    memory = id;
    try {
      localStorage.setItem(KEY, id);
    } catch {
      // storage unavailable: keep the in-memory copy
    }
  },
};
