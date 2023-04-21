import { toastPlugin } from "./toast.plugin.ts";

export class HTMLDwebToastElement extends HTMLElement {
  readonly plugin = toastPlugin;
  get show() {
    return toastPlugin.show;
  }
}
customElements.define(toastPlugin.tagName, HTMLDwebToastElement);
