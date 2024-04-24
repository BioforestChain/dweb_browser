import { IPC_METHOD, IpcRequest, type $BodyData } from "../index.ts";
import { $makeExtends } from "./$makeExtends.ts";
import { fetchExtends } from "./$makeFetchExtends.ts";

// ipcRequest to Request
export function toRequest(ipcRequest: IpcRequest) {
  const method = ipcRequest.method;
  let body: undefined | $BodyData = "";
  if (method === IPC_METHOD.GET || method === IPC_METHOD.HEAD) {
    return new Request(ipcRequest.url, {
      method,
      headers: ipcRequest.headers,
    });
  }
  if (ipcRequest.body) {
    body = ipcRequest.body;
  }
  /**
   * 这里的请求是这样的，要发给用户转发需要添加http
   * /barcode-scanning.sys.dweb/process?X-Dweb-Host=api.cotdemo.bfs.dweb%3A443&rotation=0&formats=QR_CODE
   */
  return new Request(`${ipcRequest.url}`, {
    method,
    headers: ipcRequest.headers,
    body,
  });
}

export function buildRequest(url: URL, init?: $BuildRequestInit) {
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
          .filter(([_, value]) => value != undefined /* null undefined 都不传输*/)
          .map(([key, value]) => {
            return [key, typeof value === "object" ? JSON.stringify(value) : String(value)] as [string, string];
          })
      );
    }
    extendsSearch.forEach((value, key) => {
      url.searchParams.append(key, value);
    });
  }
  return Object.assign(
    new Request(url, init),
    $makeExtends<Request>()({
      fetch() {
        return Object.assign(fetch(this), fetchExtends);
      },
    })
  );
}

export interface $BuildRequestInit extends RequestInit {
  search?:
    | ConstructorParameters<typeof URLSearchParams>[0]
    // deno-lint-ignore no-explicit-any
    | Record<string, any>;
  base?: string;
  pathPrefix?: string;
}
