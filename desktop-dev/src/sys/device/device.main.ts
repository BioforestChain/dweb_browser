import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { electronConfig } from "../../helper/electronConfig.ts";

export class DeviceNMM extends NativeMicroModule {
  mmid = "device.sys.dweb" as const;

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

  _shutdown = async () => {};
}
