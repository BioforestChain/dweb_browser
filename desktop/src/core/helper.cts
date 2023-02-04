import { Ipc, IpcRequest, IpcResponse } from "./ipc.cjs";
import type {
  $Method,
  $Schema1,
  $Schema1ToType,
  $Schema2,
  $Schema2ToType,
  $TypeName1,
  $TypeName2,
  $TypeName2ToType,
} from "./types.cjs";

export const $typeNameParser = <T extends $TypeName2>(
  key: string,
  typeName2: T,
  value: string | null
) => {
  let param: any;
  if (value === null) {
    if ((typeName2 as string).endsWith("?")) {
      throw new Error(`param type error: '${key}'.`);
    } else {
      param = undefined;
    }
  } else {
    const typeName1 = (
      typeName2.endsWith("?") ? typeName2.slice(0, -1) : typeName2
    ) as $TypeName1;
    switch (typeName1) {
      case "number": {
        param = +value;
        break;
      }
      case "boolean": {
        param = value === "" ? false : Boolean(value.toLowerCase());
        break;
      }
      case "mmid": {
        if (value.endsWith(".dweb") === false) {
          throw new Error(`param mmid type error: '${key}':'${value}'`);
        }
        param = value;
        break;
      }
      case "string": {
        param = value;
        break;
      }
      case "object": {
        param = JSON.parse(value);
        break;
      }
      default:
        param = void 0;
    }
  }
  return param as $TypeName2ToType<T>;
};
export const $deserializeRequestToParams = <S extends $Schema1>(schema: S) => {
  type I = $Schema1ToType<S>;
  return (request: IpcRequest) => {
    const url = request.parsed_url;
    const params = {} as I;

    for (const [key, typeName2] of Object.entries(schema) as [
      keyof I & string,
      $TypeName2
    ][]) {
      params[key] = $typeNameParser(
        key,
        typeName2,
        url.searchParams.get(key)
      ) as never;
    }
    return params;
  };
};
/**
 * @TODO 实现模式匹配
 */
export const $serializeResultToResponse = <S extends $Schema2>(schema: S) => {
  type O = $Schema2ToType<S>;
  return (request: IpcRequest, result: O, ipc: Ipc) => {
    if (result instanceof Response) {
      return IpcResponse.fromResponse(request.req_id, result, ipc);
    }
    if (ArrayBuffer.isView(result) || result instanceof ArrayBuffer) {
      return IpcResponse.fromBinary(request.req_id, 200, result);
    }
    return IpcResponse.fromJson(request.req_id, 200, result);
  };
};

export class PromiseOut<T> {
  public resolve!: (value: T | PromiseLike<T>) => void;
  public reject!: (reason?: any) => void;
  readonly promise = new Promise<T>((resolve, reject) => {
    this.resolve = resolve;
    this.reject = reject;
  });
}

export const openNwWindow = (
  url: `${string}.html`,
  options?: nw.IWindowOptions
) => {
  return new Promise<nw.Window>((resolve) => {
    nw.Window.open(url, options, resolve);
  });
};

/** 将 request 参数解构 成 ipcRequest 的参数 */
export const $readRequestAsIpcRequest = async (request_init: RequestInit) => {
  let body: Uint8Array | ReadableStream<Uint8Array> | "" = "";
  const method = request_init.method ?? "GET";

  /// 读取 body
  if (method === "POST" || method === "PUT") {
    if (request_init.body instanceof ReadableStream) {
      body = request_init.body;
      // const reader = (
      //   request_init.body as ReadableStream<Uint8Array>
      // ).getReader();
      // const chunks: Uint8Array[] = [];
      // while (true) {
      //   const item = await reader.read();
      //   if (item.done) {
      //     break;
      //   }
      //   chunks.push(item.value);
      // }
      // buffer = Buffer.concat(chunks);
    } else if (request_init.body instanceof Blob) {
      body = new Uint8Array(await request_init.body.arrayBuffer());
    } else if (ArrayBuffer.isView(request_init.body)) {
      body =
        request_init.body instanceof Uint8Array
          ? request_init.body
          : new Uint8Array(
              request_init.body.buffer,
              request_init.body.byteOffset,
              request_init.body.byteLength
            );
    } else if (request_init.body instanceof ArrayBuffer) {
      body = Buffer.from(request_init.body);
    } else if (typeof request_init.body === "string") {
      body = simpleEncoder(request_init.body, "utf8");
    } else if (request_init.body) {
      throw new Error(
        `unsupport body type: ${request_init.body.constructor.name}`
      );
    }
  }

  /// 读取 headers
  const headers = headersToRecord(request_init.headers);

  return { method, body, headers } satisfies Pick<
    IpcRequest,
    "method" | "body" | "headers"
  >;
};

export const headersToRecord = (headers?: HeadersInit | null) => {
  let record: Record<string, string> = Object.create(null);
  if (headers) {
    let req_headers: Headers | undefined;
    if (headers instanceof Array) {
      req_headers = new Headers(headers);
    } else if (headers instanceof Headers) {
      req_headers = headers;
    } else {
      record = headers;
    }
    if (req_headers !== undefined) {
      req_headers.forEach((value, key) => {
        record[key] = value;
      });
    }
  }
  return record;
};

/** 将 fetch 的参数进行标准化解析 */
export const normalizeFetchArgs = (
  url: RequestInfo | URL,
  init?: RequestInit
) => {
  let _parsed_url: URL | undefined;
  let _request_init = init;
  if (typeof url === "string") {
    _parsed_url = new URL(url, location.href);
  } else if (url instanceof Request) {
    _parsed_url = new URL(url.url);
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

type $Helpers<M> = M & ThisType<Promise<Response> & M>; // Type of 'this' in methods is D & M
const $make_helpers = <M extends unknown>(helpers: $Helpers<M>) => {
  return helpers;
};
export const fetch_helpers = $make_helpers({
  async number() {
    const text = await this.string();
    return +text;
  },
  async string() {
    const response = await this;
    return response.text();
  },
  async boolean() {
    const text = await this.string();
    return text === "true"; // JSON.stringify(true)
  },
  async object<T>() {
    const response = await this;
    try {
      const object = (await response.json()) as T;
      return object;
    } catch (err) {
      debugger;
      throw err;
    }
  },
  async *jsonlines<T = unknown>() {
    const stream = await this.then((res) => {
      const stream = res.body;
      if (stream == null) {
        throw new Error(`request ${res.url} could not by stream.`);
      }
      return stream;
    });
    const reader = stream.getReader();

    let json = "";
    try {
      while (true) {
        const item = await reader.read();
        if (item.done) {
          break;
        }
        json += simpleDecoder(item.value, "utf8");
        while (json.includes("\n")) {
          const line_break_index = json.indexOf("\n");
          const line = json.slice(0, line_break_index);
          yield JSON.parse(line) as T;
          json = json.slice(line.length + 1);
        }
      }
      json = json.trim();
      if (json.length > 0) {
        yield JSON.parse(json) as T;
      }
    } catch (err) {
      debugger;
    }
  },
});

export type $SimpleEncoding = "utf8" | "base64";
const textEncoder = new TextEncoder();
export const simpleEncoder = (data: string, encoding: $SimpleEncoding) => {
  if (encoding === "base64") {
    const byteCharacters = atob(data);
    const binary = new Uint8Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
      binary[i] = byteCharacters.charCodeAt(i);
    }
    return binary;
  }
  return textEncoder.encode(data);
};
const textDecoder = new TextDecoder();
export const simpleDecoder = (
  data: ArrayBuffer | ArrayBufferView,
  encoding: $SimpleEncoding
) => {
  if (encoding === "base64") {
    let binary = "";

    const bytes =
      "buffer" in data
        ? data instanceof Uint8Array
          ? data
          : new Uint8Array(data.buffer, data.byteOffset, data.byteLength)
        : new Uint8Array(data);
    for (const byte of bytes) {
      binary += String.fromCharCode(byte);
    }
    return btoa(binary);
  }
  return textDecoder.decode(data);
};

export const utf8_to_b64 = (str: string) => {
  return btoa(unescape(encodeURIComponent(str)));
};

export const b64_to_utf8 = (str: string) => {
  return decodeURIComponent(escape(atob(str)));
};

export const isBinary = (
  data: unknown
): data is ArrayBuffer | ArrayBufferView =>
  data instanceof ArrayBuffer || ArrayBuffer.isView(data);

export const createSingle = <
  $Callback extends (...args: any[]) => unknown
>() => {
  const cbs = new Set<$Callback>();
  const bind = (cb: $Callback) => {
    cbs.add(cb);
    return () => cbs.delete(cb);
  };
  const emit = (...args: Parameters<$Callback>) => {
    for (const cb of cbs) {
      cb.apply(null, args);
    }
  };
  return { bind, emit } as const;
};

export interface $ReqMatcher {
  readonly pathname: string;
  readonly matchMode: "full" | "prefix";
  readonly method?: $Method;
}

export const $isMatchReq = (
  matcher: $ReqMatcher,
  pathname: string,
  method: string = "GET"
) => {
  return (
    (matcher.method ?? "GET") === method &&
    (matcher.matchMode === "full"
      ? pathname === matcher.pathname
      : matcher.matchMode === "prefix"
      ? pathname.startsWith(matcher.pathname)
      : false)
  );
};

export const dataUrlFromUtf8 = (
  utf8_string: string,
  asBase64: boolean,
  mime: string = ""
) => {
  const data_url = asBase64
    ? `data:${mime};base64,${utf8_to_b64(utf8_string)}`
    : `data:${mime};charset=UTF-8,${encodeURIComponent(utf8_string)}`;
  return data_url;
};

export const createJsBlob = (code: string) => {
  const blob = new Blob([code], { type: "application/javascript" });
  const blob_url = URL.createObjectURL(blob);
  return blob_url;
};

export const wrapCommonJsCode = (
  common_js_code: string,
  options: {
    before?: string;
    after?: string;
  } = {}
) => {
  const { before = "", after = "" } = options;

  return `${before};((module,exports=module.exports)=>{${common_js_code.replaceAll(
    `"use strict";`,
    ""
  )};return module.exports})({exports:{}})${after};`;
};
