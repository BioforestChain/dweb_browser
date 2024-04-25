import { cacheGetter } from "../../helper/cacheGetter.ts";
import { mediaPlugin } from "./media.plugin.ts";

export class HTMLDwebMediaElement extends HTMLElement {
  static readonly tagName = "dweb-media";
  readonly plugin = mediaPlugin;

  @cacheGetter()
  get savePictures() {
    return this.plugin.savePictures;
  }
}

if (!customElements.get(HTMLDwebMediaElement.tagName)) {
  customElements.define(HTMLDwebMediaElement.tagName, HTMLDwebMediaElement);
}
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebMediaElement.tagName]: HTMLDwebMediaElement;
  }
}
