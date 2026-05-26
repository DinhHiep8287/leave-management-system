const REFRESH_KEY = "leave.refreshToken";

let accessTokenInMemory: string | null = null;

export const tokenStorage = {
  getAccess(): string | null {
    return accessTokenInMemory;
  },
  setAccess(token: string | null): void {
    accessTokenInMemory = token;
  },
  getRefresh(): string | null {
    try {
      return window.localStorage.getItem(REFRESH_KEY);
    } catch {
      return null;
    }
  },
  setRefresh(token: string | null): void {
    try {
      if (token) window.localStorage.setItem(REFRESH_KEY, token);
      else window.localStorage.removeItem(REFRESH_KEY);
    } catch {
      // ignore — storage may be blocked
    }
  },
  clear(): void {
    accessTokenInMemory = null;
    this.setRefresh(null);
  },
};
