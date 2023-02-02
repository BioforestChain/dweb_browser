import { IpcRequest, IpcResponse } from "./ipc.cjs";
import {
  $TypeName2,
  $TypeName1,
  $TypeName2ToType,
  $Schema1,
  $Schema1ToType,
  $Schema2,
  $Schema2ToType,
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
  return (request: IpcRequest, result: O) => {
    return new IpcResponse(request.req_id, 200, JSON.stringify(result), {
      "Content-Type": "application/json",
    });
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
export const readRequestAsIpcRequest = async (request_init: RequestInit) => {
  let body = "";
  const method = request_init.method ?? "GET";

  /// 读取 body
  if (method === "POST" || method === "PUT") {
    let buffer: Buffer | undefined;
    if (request_init.body instanceof ReadableStream) {
      const reader = (
        request_init.body as ReadableStream<Uint8Array>
      ).getReader();
      const chunks: Uint8Array[] = [];
      while (true) {
        const item = await reader.read();
        if (item.done) {
          break;
        }
        chunks.push(item.value);
      }
      buffer = Buffer.concat(chunks);
    } else if (request_init.body instanceof Blob) {
      buffer = Buffer.from(await request_init.body.arrayBuffer());
    } else if (ArrayBuffer.isView(request_init.body)) {
      buffer = Buffer.from(
        request_init.body.buffer,
        request_init.body.byteOffset,
        request_init.body.byteLength
      );
    } else if (request_init.body instanceof ArrayBuffer) {
      buffer = Buffer.from(request_init.body);
    } else if (typeof request_init.body === "string") {
      body = request_init.body;
    } else if (request_init.body) {
      throw new Error(
        `unsupport body type: ${request_init.body.constructor.name}`
      );
    }
    if (buffer !== undefined) {
      body = buffer.toString("base64");
    }
  }

  /// 读取 headers
  let headers: Record<string, string> = Object.create(null);
  if (request_init.headers) {
    let req_headers: Headers | undefined;
    if (request_init.headers instanceof Array) {
      req_headers = new Headers(request_init.headers);
    } else if (request_init.headers instanceof Headers) {
      req_headers = request_init.headers;
    } else {
      headers = request_init.headers;
    }
    if (req_headers !== undefined) {
      req_headers.forEach((value, key) => {
        headers[key] = value;
      });
    }
  }

  return { method, body, headers };
};

/** 将 fetch 的参数进行标准化解析 */
export const normalizeFetchArgs = (
  url: RequestInfo | URL,
  init?: RequestInit
) => {
  let _parsed_url: URL | undefined;
  let _request_init = init;
  if (typeof url === "string") {
    _parsed_url = new URL(url);
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
