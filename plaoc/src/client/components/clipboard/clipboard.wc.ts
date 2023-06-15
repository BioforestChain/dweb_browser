import { cacheGetter } from "../../helper/cacheGetter.ts";
import { clipboardPlugin } from "./clipboard.plugin.ts";

export class HTMLDwebClipboardElement extends HTMLElement {
  static readonly tagName = "dweb-clipboard";
  readonly plugin = clipboardPlugin;

  @cacheGetter()
  get read() {
    return this.plugin.read;
  }

  @cacheGetter()
  get write() {
    return this.plugin.write;
  }
}

customElements.define(
  HTMLDwebClipboardElement.tagName,
  HTMLDwebClipboardElement
);
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebClipboardElement.tagName]: HTMLDwebClipboardElement;
  }
}
