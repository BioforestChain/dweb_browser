export const getBaseUrl = () =>
  (URL_BASE ??=
    "document" in globalThis
      ? document.baseURI
      : "location" in globalThis &&
        (location.protocol === "http:" ||
          location.protocol === "https:" ||
          location.protocol === "file:" ||
          location.protocol === "chrome-extension:")
      ? location.href
      : "file:///");
let URL_BASE: undefined | string;

export const parseUrl = (url: string | URL, base = getBaseUrl()) => {
  return new URL(url, base);
};

export const updateUrlOrigin = (url: string | URL, new_origin: string) => {
  const { origin, href } = parseUrl(url);
  return new URL(new_origin + href.slice(origin.length));
};

export const appendUrlSearchs = (
  url: URL,
  extQuerys: Iterable<[string, string]>
) => {
  for (const [key, value] of extQuerys) {
    if (url.searchParams.has(key) === false) {
      url.searchParams.set(key, value);
    }
  }
  return url;
};

export const buildUrl = (
  url: URL,
  ext: {
    search?: string | URLSearchParams | Record<string, unknown> | {};
    pathname?: string;
  }
) => {
  if (ext.pathname !== undefined) {
    url.pathname = ext.pathname;
  }
  if (ext.search) {
    if (ext.search instanceof URLSearchParams) {
      url.search = ext.search.toString();
    } else if (typeof ext.search === "string") {
      url.search = ext.search.toString();
    } else {
      url.search = new URLSearchParams(
        Object.entries(ext.search).map(([key, value]) => {
          return [
            key,
            typeof value === "string" ? value : JSON.stringify(value),
          ] as [string, string];
        })
      ).toString();
    }
  }
  return url;
};
