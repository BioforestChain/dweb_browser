import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { $GeolocationPosition } from "./geolocation.type.ts";

export class GeolocationPlugin extends BasePlugin {
  constructor() {
    super("geolocation.sys.dweb");
  }

  @bindThis
  getLocation() {
    return this.fetchApi(`/location`).object<$GeolocationPosition>();
  }
}

export const geolocationPlugin = new GeolocationPlugin();
