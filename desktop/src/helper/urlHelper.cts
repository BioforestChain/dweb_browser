const URL_BASE =
  "location" in globalThis &&
  (location.protocol === "http:" ||
    location.protocol === "https:" ||
    location.protocol === "file:" ||
    location.protocol === "chrome-extension:")
    ? location.href
    : "http://localhost";

export const parseUrl = (url: string | URL, base = URL_BASE) => {
  return new URL(url, base);
};

export const updateUrlOrigin = (url: string | URL, new_origin: string) => {
  const { origin, href } = parseUrl(url);
  return new URL(new_origin + href.slice(origin.length));
};
