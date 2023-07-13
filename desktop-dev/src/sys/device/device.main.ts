import { match } from "ts-pattern";
import { IPC_METHOD } from "../../core/ipc/const.ts";
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
    this.onFetch(async (event) => {
      return match(event)
        .with({ method: IPC_METHOD.GET, pathname: "/uuid" }, () => {
          // resultResolve(Response.json({ uuid: this.uuid }));
          return Response.json({ uuid: this.uuid });
        })
        .run();
    });
  };

  _shutdown = async () => {};
}
