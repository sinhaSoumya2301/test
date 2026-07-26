/**
 * Access token lives in memory only (module-level variable) — never localStorage — to limit
 * exposure if an XSS bug ever occurs. The refresh token is persisted in localStorage so a page
 * reload doesn't force a re-login; that's a deliberate, documented trade-off (see
 * docs/adr/0003-jwt-access-refresh-auth.md) offset by the refresh token being single-use,
 * rotated, and server-revocable.
 */
const REFRESH_TOKEN_KEY = "shiftswap.refreshToken";

let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setRefreshToken(token: string | null): void {
  if (token) {
    localStorage.setItem(REFRESH_TOKEN_KEY, token);
  } else {
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  }
}

export function clearTokens(): void {
  setAccessToken(null);
  setRefreshToken(null);
}
