import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/base.plugin.ts";
import { $GeolocationContoller, $GeolocationPosition, $LocationOptions } from "./geolocation.type.ts";

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
   * 创建对位置的监听控制器
   * (会自动跟用户申请权限)
   * @param fps 位置更新之间的最小时间间隔（以毫秒为单位） default 3000
   * @param precise 是否使用精确位置 default false
   * @returns Promise<$GeolocationContoller>
   */
  async createLocation(option?: $LocationOptions): Promise<$GeolocationContoller> {
    const ws = await this.buildChannel("/location", {
      search: {
        fps: option?.fps,
        precise: option?.precise,
        minUpdateDistance: option?.minUpdateDistance,
      },
    });
    const controller = {
      listen(callback: (position: $GeolocationPosition) => void) {
        ws.onmessage = async (ev) => {
          const data = typeof ev.data === "string" ? ev.data : await (ev.data as Blob).text();
          if (data) {
            const res = JSON.parse(data) as $GeolocationPosition;
            callback(res);
          }
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
