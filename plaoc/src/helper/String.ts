/**
 * 把链接转化为url
 * @param url
 * @returns
 */
export function convertToHttps(url: string): URL {
  if (url.startsWith("http://")) {
    return new URL(url);
  }

  if (url.startsWith("/")) {
    return new URL("https:/" + url);
  }

  if (!url.startsWith("https://")) {
    return new URL("https://" + url);
  }
  return new URL(url);
}
