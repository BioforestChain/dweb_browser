import { haptics } from "../../browser/multi-webview/multi-webview.mobile.handler.ts";
import type { Ipc, IpcRequest } from "../../core/ipc/index.ts";
import type { $Schema1, $Schema1ToType } from "../../helper/types.ts";
import type { HapticsNMM } from "./haptics.main.ts";

export async function setHaptics(
  this: HapticsNMM,
  _args: $Schema1ToType<$Schema1>,
  _client_ipc: Ipc,
  ipcRequest: IpcRequest
) {
  // const search = querystring.unescape(ipcRequest.url).split("?")[1]
  const action = ipcRequest.parsed_url.pathname.slice(1);
  return await haptics.call(
    this,
    { ..._args, action },
    _client_ipc,
    ipcRequest
  );
}
