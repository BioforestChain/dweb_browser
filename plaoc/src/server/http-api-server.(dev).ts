import { X_PLAOC_QUERY } from "./const.ts";
import {
  $Ipc,
  $IpcRequest,
  $IpcResponse,
  $MMID,
  IPC_ROLE,
  IpcResponse,
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

  protected override async _onApi(request: $IpcRequest, httpServerIpc: $Ipc) {
    const sessionId = request.parsed_url.searchParams.get(
      X_PLAOC_QUERY.SESSION_ID
    );
    if (!sessionId) {
      throw new Error("no found sessionId");
    }
    if (request.parsed_url.pathname === EMULATOR_PREFIX) {
      const type = request.parsed_url.searchParams.get("type");
      const mmid = request.parsed_url.searchParams.get("mmid") as $MMID;
      if (type === "server2client") {
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

        httpServerIpc.postMessage(
          IpcResponse.fromStream(
            request.req_id,
            200,
            undefined,
            streamIpc.stream,
            httpServerIpc
          )
        );
        return;
      } else if (type == "client2server") {
        const duplex = await getConncetdDuplexPo(sessionId, mmid).promise;
        duplex.streamOut.controller.enqueue(await request.body.u8a());
        httpServerIpc.postMessage(
          IpcResponse.fromText(
            request.req_id,
            200,
            undefined,
            "",
            httpServerIpc
          )
        );
      }
    } else {
      super._onApi(request, httpServerIpc, (mmid) =>
        getConncetdIpc(sessionId, mmid)
      );
    }
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
