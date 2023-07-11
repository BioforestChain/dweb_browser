import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import type { $ResponseUUIDData } from "./device.type.ts";

export class DevicePlugin extends BasePlugin {
  constructor() {
    super("device.sys.dweb");
  }

  @bindThis
  async getUUID(): Promise<$ResponseUUIDData> {
    const res = await (await this.fetchApi(`/uuid`)).json();
    return res;
  }
}

export const devicePlugin = new DevicePlugin();
