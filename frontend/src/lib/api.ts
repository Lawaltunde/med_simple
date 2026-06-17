// Thin API client for the MedSimple backend.
// Access token lives in memory (set by the auth provider). On a 401 it attempts a
// single silent refresh via the registered handler, then retries the request once.

const BASE = "/api/v1";

let accessToken: string | null = null;
let refreshHandler: (() => Promise<string | null>) | null = null;

export const tokenStore = {
  get: () => accessToken,
  set: (t: string | null) => {
    accessToken = t;
  },
};

export function registerRefreshHandler(fn: (() => Promise<string | null>) | null) {
  refreshHandler = fn;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public code?: string
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function raw(path: string, options: RequestInit, withAuth: boolean): Promise<Response> {
  return fetch(BASE + path, {
    ...options,
    // Send the httpOnly refresh cookie on same-origin auth calls (login/refresh/logout).
    credentials: "same-origin",
    headers: {
      "Content-Type": "application/json",
      ...(withAuth && accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...options.headers,
    },
  });
}

export async function api<T = unknown>(
  path: string,
  options: RequestInit = {},
  opts: { auth?: boolean } = { auth: true }
): Promise<T> {
  const withAuth = opts.auth !== false;
  let res = await raw(path, options, withAuth);

  // One silent-refresh + retry on an expired access token.
  if (res.status === 401 && withAuth && refreshHandler) {
    const fresh = await refreshHandler();
    if (fresh) {
      accessToken = fresh;
      res = await raw(path, options, true);
    }
  }

  if (!res.ok) {
    const body = (await res.json().catch(() => ({}))) as { message?: string; error?: string };
    throw new ApiError(res.status, body.message ?? res.statusText, body.error);
  }

  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}
