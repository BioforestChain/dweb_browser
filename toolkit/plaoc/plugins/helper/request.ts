import { $makeExtends } from "./$makeExtends.ts";
import { fetchExtends } from "./$makeFetchExtends.ts";

/**构造request */
export function buildRequest(url: URL, init?: $BuildRequestInit) {
  buildSearch(init?.search, (key, value) => {
    url.searchParams.append(key, value);
  });
  return Object.assign(
    new Request(url, init),
    $makeExtends<Request>()({
      fetch() {
        return Object.assign(fetch(this), fetchExtends);
      },
    })
  );
}

/**构造search对象 */
export function buildSearch(search: $search, callback: (key: string, value: string) => void) {
  if (search) {
    let extendsSearch: URLSearchParams;
    if (search instanceof URLSearchParams) {
      extendsSearch = search;
    } else if (typeof search === "string") {
      extendsSearch = new URLSearchParams(search);
    } else {
      extendsSearch = new URLSearchParams(
        Object.entries(search)
          .filter(([_, value]) => value != undefined /* null undefined 都不传输*/)
          .map(([key, value]) => {
            return [key, typeof value === "object" ? JSON.stringify(value) : String(value)] as [string, string];
          })
      );
    }
    extendsSearch.forEach((value, key) => {
      callback(key, value);
    });
  }
}

export interface $BuildRequestInit extends RequestInit {
  search?: $search;
  base?: string;
  pathPrefix?: string;
}
export type $search =
  | ConstructorParameters<typeof URLSearchParams>[0]
  // deno-lint-ignore no-explicit-any
  | Record<string, any>;
