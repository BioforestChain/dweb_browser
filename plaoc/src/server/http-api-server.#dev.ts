import { $Ipc, $IpcRequest } from "./deps.ts";
import { Server_api as _Server_api } from "./http-api-server.ts";
const EMULATOR_PREFIX = "/emulator";
export class Server_api extends _Server_api {
  protected override async _onApi(request: $IpcRequest, httpServerIpc: $Ipc) {
    console.log("Server_api=>", request.parsed_url);
    if (request.parsed_url.pathname.startsWith(EMULATOR_PREFIX)) {
    }
    super._onApi(request, httpServerIpc);
  }
}
