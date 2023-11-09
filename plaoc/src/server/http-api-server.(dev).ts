import type { $MicroModuleManifest, $ReadableStreamIpc } from "npm:@dweb-browser/js-process@0.1.4";
import {
  $IpcResponse,
  $MMID,
  $ReadableStreamOut,
  FetchEvent,
  IPC_ROLE,
  PromiseOut,
  ReadableStreamIpc,
  jsProcess,
  mapHelper,
  simpleEncoder
} from "npm:@dweb-browser/js-process@0.1.4";
import { X_PLAOC_QUERY } from "./const.ts";
import { Server_api as _Server_api } from "./http-api-server.ts";
const EMULATOR_PREFIX = "/emulator";
export class Server_api extends _Server_api {
  readonly streamMap = new Map<string, $ReadableStreamOut<Uint8Array>>();
  readonly responseMap = new Map<number, PromiseOut<$IpcResponse>>();
  readonly jsonlineEnd = simpleEncoder("\n", "utf8");

  protected override async _onApi(event: FetchEvent) {
    const sessionId = event.searchParams.get(X_PLAOC_QUERY.SESSION_ID);
    /// 模拟器建立连接
    if (event.pathname === EMULATOR_PREFIX) {
      if (!sessionId) {
        throw new Error("no found sessionId");
      }
      const mmid = event.searchParams.get("mmid") as $MMID;

      const streamIpc = new ReadableStreamIpc(
        {
          mmid: mmid,
          name: mmid,
          ipc_support_protocols: {
            cbor: false,
            protobuf: false,
            raw: false,
          },
          dweb_deeplinks: [],
          categories: [],
        } satisfies $MicroModuleManifest,
        // @ts-ignore
        IPC_ROLE.SERVER
      );
      void streamIpc.bindIncomeStream(event.body!);

      /// force Get
      forceGetDuplex(sessionId, mmid).resolve({
        streamIpc,
      });

      /// 返回读写这个stream的链接，注意，目前双工需要客户端通过 WebSocket 来达成支持
      return { body: streamIpc.stream };
    }
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

const forceGetDuplex = (sessionId: string, mmid: $MMID) =>
  mapHelper.getOrPut(
    mapHelper.getOrPut(emulatorDuplexs, sessionId, () => new Map() as $MmidDuplexMap),
    mmid,
    () => new PromiseOut()
  );
const getConncetdIpc = (sessionId: string, mmid: $MMID) =>
  emulatorDuplexs
    .get(sessionId)
    ?.get(mmid)
    ?.promise.then((duplex) => duplex.streamIpc);
