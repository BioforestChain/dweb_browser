import type { $ReadableStreamIpc } from "npm:@dweb-browser/js-process@0.1.6";
import {
  $IpcResponse,
  $MMID,
  $ReadableStreamOut,
  FetchEvent,
  PromiseOut,
  jsProcess,
  simpleEncoder
} from "npm:@dweb-browser/js-process@0.1.6";
import { X_PLAOC_QUERY } from "./const.ts";
import { Server_api as _Server_api } from "./http-api-server.ts";
export class Server_api extends _Server_api {
  readonly streamMap = new Map<string, $ReadableStreamOut<Uint8Array>>();
  readonly responseMap = new Map<number, PromiseOut<$IpcResponse>>();
  readonly jsonlineEnd = simpleEncoder("\n", "utf8");

  protected override async _onApi(event: FetchEvent) {
    const sessionId = event.searchParams.get(X_PLAOC_QUERY.SESSION_ID);
    const fun = (mmid: $MMID) => {
      if (sessionId) {
        return getConncetdIpc(sessionId, mmid) ?? jsProcess.connect(mmid);
      }
      return jsProcess.connect(mmid);
    };
    /// 请求模拟器或者直接请求原生
    return super._onApi(event, fun, false);
  }
}

type $MmidDuplexMap = Map<
  $MMID,
  PromiseOut<{
    streamIpc: $ReadableStreamIpc;
  }>
>;
export const emulatorDuplexs = new Map<string, $MmidDuplexMap>();

const getConncetdIpc = (sessionId: string, mmid: $MMID) =>
  emulatorDuplexs
    .get(sessionId)
    ?.get(mmid)
    ?.promise.then((duplex) => duplex.streamIpc);
