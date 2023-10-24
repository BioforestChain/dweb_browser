import { cacheGetter } from "../../helper/cacheGetter.ts";
import { toastPlugin } from "./toast.plugin.ts";

export class HTMLDwebToastElement extends HTMLElement {
  static readonly tagName = "dweb-toast";
  readonly plugin = toastPlugin;

  @cacheGetter()
  get show() {
    return toastPlugin.show;
  }
}

if (!customElements.get(HTMLDwebToastElement.tagName)) {
  customElements.define(HTMLDwebToastElement.tagName, HTMLDwebToastElement);
}
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebToastElement.tagName]: HTMLDwebToastElement;
  }
}
