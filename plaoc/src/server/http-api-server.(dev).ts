import { X_EMULATOR_ACTION, X_PLAOC_QUERY } from "./const.ts";
import {
  $IpcResponse,
  $MMID,
  FetchError,
  FetchEvent,
  IPC_ROLE,
  PromiseOut,
  ReadableStreamIpc,
  ReadableStreamOut,
  mapHelper,
  simpleEncoder,
} from "./deps.ts";
import { Server_api as _Server_api } from "./http-api-server.ts";
const EMULATOR_PREFIX = "/emulator";
export class Server_api extends _Server_api {
  readonly streamMap = new Map<string, ReadableStreamOut<Uint8Array>>();
  readonly responseMap = new Map<number, PromiseOut<$IpcResponse>>();
  readonly jsonlineEnd = simpleEncoder("\n", "utf8");

  protected override async _onApi(event: FetchEvent) {
    const sessionId = event.searchParams.get(X_PLAOC_QUERY.SESSION_ID);
    if (!sessionId) {
      throw new Error("no found sessionId");
    }
    if (event.pathname === EMULATOR_PREFIX) {
      const type = event.searchParams.get("type");
      const mmid = event.searchParams.get("mmid") as $MMID;
      if (type === X_EMULATOR_ACTION.SERVER_2_CLIENT) {
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
        const streamOut = new ReadableStreamOut<Uint8Array>();
        streamIpc.bindIncomeStream(streamOut.stream);
        streamIpc.onClose(() => {
          streamOut.controller.close();
        });
        getConncetdDuplexPo(sessionId, mmid).resolve({ streamIpc, streamOut });

        return { body: streamIpc.stream };
      } else if (type == X_EMULATOR_ACTION.CLIENT_2_SERVER) {
        const duplex = await getConncetdDuplexPo(sessionId, mmid).promise;
        duplex.streamOut.controller.enqueue(await event.typedArray());
        return { body: "" };
      }
      throw new FetchError(`invalid type: ${type}`);
    }
    return super._onApi(event, (mmid) => getConncetdIpc(sessionId, mmid));
  }
}

type $MmidDuplexMap = Map<
  $MMID,
  PromiseOut<{
    streamIpc: ReadableStreamIpc;
    streamOut: ReadableStreamOut<Uint8Array>;
  }>
>;
export const emulatorDuplexs = new Map<string, $MmidDuplexMap>();
const getConncetdDuplexPo = (sessionId: string, mmid: $MMID) =>
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
  getConncetdDuplexPo(sessionId, mmid).promise.then(
    (duplex) => duplex.streamIpc
  );
