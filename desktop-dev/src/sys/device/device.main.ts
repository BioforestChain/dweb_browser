import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { electronConfig } from "../../helper/electronConfig.ts";

export class DeviceNMM extends NativeMicroModule {
  mmid = "device.sys.dweb" as const;
  // private _onFetchAdapter = new OnFetchAdapter();
  // private _responseHeader = new IpcHeaders().init(
  //   "Content-Type",
  //   "application/json"
  // );

  get uuid() {
    let _uuid = electronConfig.get("device-uuid");
    if (_uuid) {
      return _uuid;
    }
    _uuid = crypto.randomUUID();
    electronConfig.set("device-uuid", _uuid);
    return _uuid;
  }

  _bootstrap = async () => {
    console.log("", this.mmid, this.uuid);
    // this._onFetchAdapterInit();
    this.onFetch((event) => {
      const { pathname } = event;
      if (pathname === "/uuid") {
        return Response.json({ uuid: this.uuid });
      }
    });
  };

  // private _onFetchAdapterInit = async () => {
  //   this._onFetchAdapter.add("GET", "/uuid", this._getUUIDHandler);
  // };

  // private _getUUIDHandler = async (event: FetchEvent) => {
  //   return IpcResponse.fromJson(
  //     event.ipcRequest.req_id,
  //     200,
  //     this._responseHeader,
  //     JSON.stringify({ uuid: this.uuid }),
  //     event.ipc
  //   );
  // };

  _shutdown = async () => {};
}
