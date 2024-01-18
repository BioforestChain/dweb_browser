import { cacheGetter } from "../../helper/cacheGetter.ts";
import { cameraPlugin } from "./camera.plugin.ts";
export class HTMLCameraElement extends HTMLElement {
  static readonly tagName = "dweb-camera";
  plugin = cameraPlugin;

  @cacheGetter()
  get check() {
    return this.plugin.getPhoto;
  }
}

// 注册
if (!customElements.get(HTMLCameraElement.tagName)) {
  customElements.define(HTMLCameraElement.tagName, HTMLCameraElement);
}

declare global {
  interface HTMLElementTagNameMap {
    [HTMLCameraElement.tagName]: HTMLCameraElement;
  }
}
