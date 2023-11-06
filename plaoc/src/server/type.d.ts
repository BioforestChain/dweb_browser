declare global {
  interface CookieStore {
    getAll(): Promise<ChromeCookie[]>;
    delete(name: string): ChromeCookie;
  }

  interface ChromeCookie {
    name: string;
    value: string;
    domain: string;
    path: string;
    expires: number;
    size: number;
    httpOnly: boolean;
    secure: boolean;
    session: boolean;
  }

  const cookieStore: CookieStore;
}

export { };
