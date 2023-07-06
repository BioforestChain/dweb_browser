import { X_PLAOC_QUERY } from "./const.ts";
import {
  $IpcResponse,
  $MMID,
  $OnFetchReturn,
  FetchEvent,
  IPC_ROLE,
  PromiseOut,
  ReadableStreamIpc,
  ReadableStreamOut,
  jsProcess,
  mapHelper,
  simpleEncoder,
} from "./deps.ts";
import { Server_api as _Server_api } from "./http-api-server.ts";
const EMULATOR_PREFIX = "/emulator";
export class Server_api extends _Server_api {
  readonly streamMap = new Map<string, ReadableStreamOut<Uint8Array>>();
  readonly responseMap = new Map<number, PromiseOut<$IpcResponse>>();
  readonly jsonlineEnd = simpleEncoder("\n", "utf8");

  /**内部请求事件 */
  protected override async _onInternal(
    event: FetchEvent
  ): Promise<$OnFetchReturn> {
    const sessionId = event.searchParams.get(X_PLAOC_QUERY.SESSION_ID);
    if (!sessionId) {
      throw new Error("session not connect!");
    }
    return super._onInternal(
      event,
      (mmid) => getConncetdIpc(sessionId, mmid) ?? jsProcess.connect(mmid)
    );
  }

  protected override async _onApi(event: FetchEvent) {
    const sessionId = event.searchParams.get(X_PLAOC_QUERY.SESSION_ID);
    if (!sessionId) {
      throw new Error("no found sessionId");
    }
    if (event.pathname === EMULATOR_PREFIX) {
      const mmid = event.searchParams.get("mmid") as $MMID;

      const streamIpc = new ReadableStreamIpc(
        {
          mmid: mmid,
          ipc_support_protocols: {
            message_pack: false,
            protobuf: false,
            raw: false,
          },
          dweb_deeplinks: [],
        },
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
    return super._onApi(
      event,
      (mmid) => getConncetdIpc(sessionId, mmid) ?? jsProcess.connect(mmid)
    );
  }
}

type $MmidDuplexMap = Map<
  $MMID,
  PromiseOut<{
    streamIpc: ReadableStreamIpc;
  }>
>;
export const emulatorDuplexs = new Map<string, $MmidDuplexMap>();

const forceGetDuplex = (sessionId: string, mmid: $MMID) =>
  mapHelper.getOrPut(
    mapHelper.getOrPut(
      emulatorDuplexs,
      sessionId,
      () => new Map() as $MmidDuplexMap
    ),
    mmid,
    () => new PromiseOut()
  );
const getConncetdIpc = (sessionId: string, mmid: $MMID) =>
  emulatorDuplexs
    .get(sessionId)
    ?.get(mmid)
    ?.promise.then((duplex) => duplex.streamIpc);
