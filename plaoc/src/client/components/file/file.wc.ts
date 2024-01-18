import { cacheGetter } from "../../helper/cacheGetter.ts";
import { filePlugin } from "./file.plugin.ts";

export class HTMLDwebFileElement extends HTMLElement {
  static readonly tagName = "dweb-file";
  readonly plugin = filePlugin;

  @cacheGetter()
  get open() {
    return this.plugin.open;
  }
}

if (!customElements.get(HTMLDwebFileElement.tagName)) {
  customElements.define(HTMLDwebFileElement.tagName, HTMLDwebFileElement);
}
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebFileElement.tagName]: HTMLDwebFileElement;
  }
}
