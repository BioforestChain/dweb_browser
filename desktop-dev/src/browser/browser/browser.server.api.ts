import { $Schema1ToType } from "../../core/helper/types.ts";
import { Ipc, IpcEvent, IpcRequest, IpcResponse } from "../../core/ipc/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { $MMID } from "../../core/types.ts";
import { getAllApps } from "../jmm/jmm.api.serve.ts";
import type { BrowserNMM } from "./browser.ts";

export async function getAppsInfo() {
  return await getAllApps();
}

export async function updateContent(this: BrowserNMM, _args: $Schema1ToType<{}>, ipc: Ipc, request: IpcRequest) {
  const href = request.parsed_url.searchParams.get("url");
  if (href === null) {
    ipc.postMessage(IpcResponse.fromText(request.req_id, 400, undefined, "缺少 url 参数", ipc));
    return;
  }
  const regexp = /^(https?|http):\/\/([a-z0-9-]+(.[a-z0-9-]+)*(:[0-9]+)?)(\/.*)?$/i;
  if (regexp.test(href)) {
    this.contentBV?.loadWithHistory(href);
    ipc.postMessage(IpcResponse.fromText(request.req_id, 200, undefined, "ok", ipc));
    return;
  }
  ipc.postMessage(IpcResponse.fromText(request.req_id, 400, undefined, "非法的 url 参数:" + href, ipc));
}

export async function canGoBack(this: BrowserNMM, _args: $Schema1ToType<{}>, ipc: Ipc, request: IpcRequest) {
  ipc.postMessage(
    await IpcResponse.fromJson(
      request.req_id,
      200,
      undefined,
      {
        value: this.contentBV?.canGoBack(),
      },
      ipc
    )
  );
}

export async function canGoForward(this: BrowserNMM, _args: $Schema1ToType<{}>, ipc: Ipc, request: IpcRequest) {
  ipc.postMessage(
    await IpcResponse.fromJson(
      request.req_id,
      200,
      undefined,
      {
        value: this.contentBV?.canGoForward(),
      },
      ipc
    )
  );
}

export async function goBack(this: BrowserNMM, _args: $Schema1ToType<{}>, ipc: Ipc, request: IpcRequest) {
  this.contentBV?.goBack();
  ipc.postMessage(
    IpcResponse.fromJson(
      request.req_id,
      200,
      undefined,
      {
        value: "ok",
      },
      ipc
    )
  );
}

export async function goForward(this: BrowserNMM, _args: $Schema1ToType<{}>, ipc: Ipc, request: IpcRequest) {
  this.contentBV?.goForward();
  ipc.postMessage(
    IpcResponse.fromJson(
      request.req_id,
      200,
      undefined,
      {
        value: "ok",
      },
      ipc
    )
  );
}

export async function refresh(this: BrowserNMM, _args: $Schema1ToType<{}>, ipc: Ipc, request: IpcRequest) {
  try {
    this.contentBV?.reload();
  } catch (err) {
    console.error("error", err);
    throw new Error(`refresh err`);
  }

  ipc.postMessage(
    IpcResponse.fromJson(
      request.req_id,
      200,
      undefined,
      {
        value: "ok",
      },
      ipc
    )
  );
}

export async function openApp(this: NativeMicroModule, mmid: $MMID) {
  if (mmid === null) {
    return "缺少 app_id 参数";
  }

  // 还需要判断 应用是否已经更新了
  const [jsIpc] = await this.context?.dns.connect(mmid as $MMID)!;
  // 如果 对应app的全部 devTools 中有没有关闭的，就无法再次打开
  jsIpc.postMessage(IpcEvent.fromText("activity", ""));
  console.always("activity", mmid);
  return true;
}
