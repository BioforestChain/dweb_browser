export { streamRead } from "../client/helper/readableStreamHelper.ts";
import { streamRead } from "../client/helper/readableStreamHelper.ts";
import { buildRequest, toRequest } from "../client/helper/request.ts";
import { IpcRequest } from "../client/index.ts";
import { X_PLAOC_QUERY } from "../server/const.ts";
export const EMULATOR = "/emulator";

export function isShadowRoot(o: ShadowRoot | unknown): o is ShadowRoot {
  return typeof o === "object" && o !== null && "host" in o && "mode" in o;
}

export function isHTMLElement(o: HTMLElement | unknown): o is HTMLElement {
  return o instanceof HTMLElement;
}

export function isCSSStyleDeclaration(
  o: CSSStyleDeclaration | unknown
): o is CSSStyleDeclaration {
  return o instanceof CSSStyleDeclaration;
}
export type EmulatorAction = "connect" | "response";

class SignalRequest {
  BASE_URL =
    new URL(location.href).searchParams.get(X_PLAOC_QUERY.INTERNAL_URL) ?? "";
  url = new URL(EMULATOR, this.BASE_URL);

  // 回复信息给后端
  respondWith(
    reqId: string,
    response: Blob | ReadableStream<Uint8Array> | string
  ) {
    return buildRequest(this.url, {
      search: {
        reqId,
        action: "response",
      },
      body: response,
    });
  }

  // 建立流通信
  async *registerConnectStream(mmid: string) {
    const jsonlines = await buildRequest(this.url, {
      search: {
        mmid: mmid,
        action: "connect",
      },
    })
      .fetch()
      .jsonlines(this.decodeIpcRequest);

    for await (const request of streamRead(jsonlines)) {
      yield request;
    }
  }

  // 转换ipcRequest对象
  decodeIpcRequest(ipcRequest: IpcRequest) {
    return new EmulatorRequest(ipcRequest.req_id, toRequest(ipcRequest));
  }
}

export const signalRequest = new SignalRequest();

export class EmulatorRequest {
  constructor(readonly req_id: string, readonly request: Request) {}
}
