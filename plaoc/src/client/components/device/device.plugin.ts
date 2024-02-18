import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/base.plugin.ts";
import type { $ResponseUUIDData } from "./device.type.ts";

export class DevicePlugin extends BasePlugin {
  constructor() {
    super("device.sys.dweb");
  }

  @bindThis
  getUUID(): Promise<$ResponseUUIDData> {
    return this.fetchApi(`/uuid`).object<$ResponseUUIDData>();
  }
}

export const devicePlugin = new DevicePlugin();
