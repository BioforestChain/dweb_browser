import { cacheGetter } from "../../helper/cacheGetter.ts";
import { clipboardPlugin } from "./clipboard.plugin.ts";

export class HTMLDwebClipboardElement extends HTMLElement {
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

customElements.define(clipboardPlugin.tagName, HTMLDwebClipboardElement);
