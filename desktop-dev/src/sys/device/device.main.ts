import crypto from "node:crypto";
import type { $OnFetch, FetchEvent } from "../../core/helper/ipcFetchHelper.ts";
import { IpcHeaders, IpcResponse } from "../../core/ipc/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { electronConfig } from "../../helper/electronConfig.ts";
import { OnFetchAdapter } from "../../helper/onFetchAdapter.ts";

export class DeviceNMM extends NativeMicroModule {
  mmid = "device.sys.dweb" as const;
  private _onFetchAdapter = new OnFetchAdapter();
  private _responseHeader = new IpcHeaders().init(
    "Content-Type",
    "application/json"
  );

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
    this._onFetchAdapterInit();
  };

  private _onFetchAdapterInit = async () => {
    this._onFetchAdapter.add("GET", "/uuid", this._getUUIDHandler);
  };

  private _getUUIDHandler: $OnFetch = async (event: FetchEvent) => {
    console.log("", "_getUUIDHandler");
    return IpcResponse.fromJson(
      event.ipcRequest.req_id,
      200,
      this._responseHeader,
      JSON.stringify({ uuid: this.uuid }),
      event.ipc
    );
  };

  _shutdown = async () => {};
}

// f4f6df7b-55d6-40cf-bc10-0dd500876551
// 1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed
// c30d54de-4ed2-4173-8f7e-3942760daa02
// 52d62e60-ab63-466a-81e3-c65720951638
/**
 * https://github.com/ungap/random-uuid/blob/main/index.js
 */
// if (typeof crypto.randomUUID !== "function") {
//   crypto.randomUUID = function randomUUID() {
//     return "10000000-1000-4000-8000-100000000000".replace(/[018]/g, (_c) => {
//       const c = +_c;
//       return (
//         c ^
//         (crypto.getRandomValues(new Uint8Array(1))[0] & (15 >> (c / 4)))
//       ).toString(16);
//     }) as never;
//   };
// }
