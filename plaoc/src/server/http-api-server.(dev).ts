import { X_EMULATOR_ACTION, X_PLAOC_QUERY } from "./const.ts";
import {
  $IpcResponse,
  $MMID,
  FetchEvent,
  HttpDwebServer,
  IPC_ROLE,
  PromiseOut,
  ReadableStreamIpc,
  ReadableStreamOut,
  http,
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
      const streamOut = new ReadableStreamOut<Uint8Array>();
      streamIpc.bindIncomeStream(streamOut.stream);
      streamIpc.onClose(() => {
        streamOut.controller.close();
      });
      /// 这里我们会根据mmid创建一个http服务，然后将链接返回
      const serverPm = http.createHttpDwebServer(jsProcess, {
        subdomain: sessionId + "." + mmid,
        port: 443,
      });
      /// force Get
      mapHelper
        .getOrPut(
          mapHelper.getOrPut(
            emulatorDuplexs,
            sessionId,
            () => new Map() as $MmidDuplexMap
          ),
          mmid,
          () => new PromiseOut()
        )
        .resolve({
          streamIpc,
          streamOut,
          serverPm,
        });

      const server = await serverPm;
      const serverProxyIpc = await server.listen();
      /// 这里http服务是为了给前端通过 get + post 来实现数据的传输，将传输的数据喂给这个 streamOut
      serverProxyIpc.onFetch(async (event) => {
        const type = event.searchParams.get("type");
        if (type === X_EMULATOR_ACTION.SERVER_2_CLIENT) {
          return {
            body: streamIpc.stream,
          };
        } else if (type == X_EMULATOR_ACTION.CLIENT_2_SERVER) {
          streamOut.controller.enqueue(await event.typedArray());
          return {
            body: "",
          };
        }
      });
      /// 返回读写这个stream的链接
      return { body: server.startResult.urlInfo.buildInternalUrl().href };
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
    serverPm: Promise<HttpDwebServer>;
    streamIpc: ReadableStreamIpc;
    streamOut: ReadableStreamOut<Uint8Array>;
  }>
>;
export const emulatorDuplexs = new Map<string, $MmidDuplexMap>();

const getConncetdIpc = (sessionId: string, mmid: $MMID) =>
  emulatorDuplexs
    .get(sessionId)
    ?.get(mmid)
    ?.promise.then((duplex) => duplex.streamIpc);
