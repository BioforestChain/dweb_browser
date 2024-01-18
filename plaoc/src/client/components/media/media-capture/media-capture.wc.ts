import { cacheGetter } from "../../../helper/cacheGetter.ts";
import { mediaCapturePlugin } from "./media-capture.plugin.ts";
export class HTMLMediaCaptureElement extends HTMLElement {
  plugin = mediaCapturePlugin;
  static readonly tagName = "dweb-media-capture";

  @cacheGetter()
  get capture() {
    return this.plugin.capture;
  }
}

// 注册
if (!customElements.get(HTMLMediaCaptureElement.tagName)) {
  customElements.define(HTMLMediaCaptureElement.tagName, HTMLMediaCaptureElement);
}

declare global {
  interface HTMLElementTagNameMap {
    [HTMLMediaCaptureElement.tagName]: HTMLMediaCaptureElement;
  }
}
