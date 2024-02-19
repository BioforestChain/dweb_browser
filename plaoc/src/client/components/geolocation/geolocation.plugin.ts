import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/base.plugin.ts";
import { $GeolocationContoller, $GeolocationPosition } from "./geolocation.type.ts";

export class GeolocationPlugin extends BasePlugin {
  constructor() {
    super("geolocation.sys.dweb");
  }

  /**
   * 单次获取定位
   * (会自动跟用户申请权限)
   * @returns Promise<$GeolocationPosition>
   */
  @bindThis
  getLocation() {
    return this.fetchApi(`/location`).object<$GeolocationPosition>();
  }

  /**
   * 创建对位置的不断监听
   * (会自动跟用户申请权限)
   * @param fps
   * @param precise
   * @returns Promise<$GeolocationContoller>
   */
  async createLocation(fps?: number, precise = false): Promise<$GeolocationContoller> {
    const ws = await this.buildChannel("/location", {
      search: {
        fps: fps,
        precise: precise,
      },
    });
    const controller = {
      listen(callback: (position: $GeolocationPosition) => void) {
        ws.onmessage = async (ev) => {
          const data = typeof ev.data === "string" ? ev.data : await (ev.data as Blob).text();
          const res = JSON.parse(data) as $GeolocationPosition;
          callback(res);
        };
      },
      stop() {
        ws.close();
      },
    };
    return controller;
  }
}

export const geolocationPlugin = new GeolocationPlugin();
