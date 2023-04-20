import { cacheGetter } from "../../helper/cacheGetter.ts";
import { biometricsPlugin } from "./biometrics.plugin.ts";

export class HTMLDwebBiometricsElement extends HTMLElement {

  @cacheGetter()
  get chuck() {
    return biometricsPlugin.check
  }

  @cacheGetter()
  get biometrics() {
    return biometricsPlugin.biometrics
  }
}
