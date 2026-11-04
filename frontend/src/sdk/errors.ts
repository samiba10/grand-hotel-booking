/** Every failed call surfaces as an ApiError - pages never deal with raw fetch errors. */
export class ApiError extends Error {
  readonly status: number;
  readonly details: string[];
  readonly transactionId?: string;

  constructor(status: number, message: string, details: string[] = [], transactionId?: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
    this.transactionId = transactionId;
  }

  /** 0 = the request never got an HTTP answer (offline, DNS, CORS). */
  get isNetwork(): boolean {
    return this.status === 0;
  }
}

export function isApiError(e: unknown): e is ApiError {
  return e instanceof ApiError;
}
