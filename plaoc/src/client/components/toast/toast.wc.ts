import { cacheGetter } from "../../helper/cacheGetter.ts";
import { toastPlugin } from "./toast.plugin.ts";

export class HTMLDwebToastElement extends HTMLElement {
  readonly plugin = toastPlugin;

  @cacheGetter()
  get show() {
    return toastPlugin.show;
  }
}
customElements.define(toastPlugin.tagName, HTMLDwebToastElement);
