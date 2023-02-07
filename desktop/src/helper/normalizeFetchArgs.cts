/** 将 fetch 的参数进行标准化解析 */

import { parseUrl } from "./urlHelper.cjs";

export const normalizeFetchArgs = (
  url: RequestInfo | URL,
  init?: RequestInit
) => {
  let _parsed_url: URL | undefined;
  let _request_init = init;
  if (typeof url === "string") {
    _parsed_url = parseUrl(url);
  } else if (url instanceof Request) {
    _parsed_url = parseUrl(url.url);
    _request_init = url;
  } else if (url instanceof URL) {
    _parsed_url = url;
  }
  if (_parsed_url === undefined) {
    throw new Error(`no found url for fetch`);
  }
  const parsed_url = _parsed_url;
  const request_init = _request_init ?? {};
  return {
    parsed_url,
    request_init,
  };
};
