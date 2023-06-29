/// about:blank
/// about:newtab
const BASE_URL =
  location.protocol === "about:" || location.protocol === "chrome:"
    ? new URL(
        `http://browser.dweb.localhost/${location.href.replace(
          new RegExp(location.protocol + "/+"),
          ""
        )}`
      )
    : new URL(location.href);
export const nativeFetch = (pathname: string, init?: $BuildRequestInit) => {
  // 默认请求自己
  const mmid = init?.mmid ?? "browser.dweb";
  const url = new URL(`api/${mmid}${pathname}`, BASE_URL);
  return buildRequest(url, init);
};

function buildRequest(url: URL, init?: $BuildRequestInit) {
  const search = init?.search;
  if (search) {
    let extendsSearch: URLSearchParams;
    if (search instanceof URLSearchParams) {
      extendsSearch = search;
    } else if (typeof search === "string") {
      extendsSearch = new URLSearchParams(search);
    } else {
      extendsSearch = new URLSearchParams(
        Object.entries(search)
          .filter(
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            ([_, value]) => value != undefined /* null undefined 都不传输*/
          )
          .map(([key, value]) => {
            return [
              key,
              typeof value === "object" ? JSON.stringify(value) : String(value),
            ] as [string, string];
          })
      );
    }
    extendsSearch.forEach((value, key) => {
      url.searchParams.append(key, value);
    });
  }
  return fetch(url, init);
}

interface $BuildRequestInit extends RequestInit {
  search?:
    | ConstructorParameters<typeof URLSearchParams>[0]
    // deno-lint-ignore no-explicit-any
    | Record<string, any>;
  base?: string;
  mmid?: `${string}.dweb`;
}
