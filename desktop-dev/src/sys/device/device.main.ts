import { match } from "ts-pattern";
import { IPC_METHOD } from "../../core/ipc/const.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { CacheGetter } from "../../helper/cacheGetter.ts";
import { electronConfig } from "../../helper/electronStore.ts";

const UUID = "device-uuid";
declare global {
  interface ElectronConfig {
    [UUID]: string;
  }
}

export class DeviceNMM extends NativeMicroModule {
  mmid = "device.sys.dweb" as const;

  #uuid = new CacheGetter(() => electronConfig.get(UUID, () => crypto.randomUUID()));
  get uuid() {
    return this.#uuid.value;
  }

  _bootstrap = async () => {
    this.onFetch(async (event) => {
      return match(event)
        .with({ method: IPC_METHOD.GET, pathname: "/uuid" }, () => {
          // resultResolve(Response.json({ uuid: this.uuid }));
          return Response.json({ uuid: this.uuid });
        })
        .run();
    }).internalServerError();
  };

  _shutdown = async () => {};
}
