import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/base.plugin.ts";
import type { $GeolocationPosition, $LocationOptions } from "./geolocation.type.ts";

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
   * @returns Promise<$GeolocationController>
   */
  async createLocation(option?: $LocationOptions) {
    const ws = await this.buildChannel("/location", {
      search: {
        precise: option?.precise,
        minDistance: option?.minDistance,
      },
    });
    return new GeolocationController(ws);
  }
}

export class GeolocationController {
  readonly #ws;
  constructor(ws: WebSocket) {
    this.#ws = ws;
  }
  listen(callback: (position: $GeolocationPosition) => void) {
    if (this.isClosed) {
      throw new Error("GeolocationController already closed");
    }
    const onmessage = async (ev: MessageEvent) => {
      const data = typeof ev.data === "string" ? ev.data : await (ev.data as Blob).text();
      if (data) {
        const res = JSON.parse(data) as $GeolocationPosition;
        callback(res);
      }
    };
    this.#ws.addEventListener("message", onmessage);
    return () => {
      this.#ws.removeEventListener("message", onmessage);
    };
  }
  get isClosed() {
    return this.#ws.readyState === WebSocket.CLOSING || this.#ws.readyState === WebSocket.CLOSED;
  }
  stop() {
    this.#ws.close();
  }
  get onclose() {
    return this.#ws.onclose;
  }
  set onclose(value) {
    this.#ws.onclose = value;
  }
}

export const geolocationPlugin = new GeolocationPlugin();
