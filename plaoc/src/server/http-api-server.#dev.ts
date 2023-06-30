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
    const url = request.parsed_url;
    console.log(
      url.searchParams.get("mmid"),
      url.searchParams.get("action"),
      request.parsed_url.pathname
    );

    if ((request.parsed_url.pathname = EMULATOR_PREFIX)) {
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
        getConncetdIpcPo(mmid).resolve(streamIpc);

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
        const streamIpc = await getConncetdIpc(mmid);
        streamIpc.bindIncomeStream(request.body.stream());
        streamIpc.onClose(() => {
          httpServerIpc.postMessage(
            IpcResponse.fromText(
              request.req_id,
              200,
              undefined,
              "",
              httpServerIpc
            )
          );
        });
      }
    } else {
      super._onApi(request, httpServerIpc, getConncetdIpc);
    }
  }
}

const emulatorIpcs = new Map<$MMID, PromiseOut<ReadableStreamIpc>>();
const getConncetdIpcPo = (mmid: $MMID) =>
  mapHelper.getOrPut(emulatorIpcs, mmid, () => new PromiseOut());
const getConncetdIpc = (mmid: $MMID) => getConncetdIpcPo(mmid).promise;
