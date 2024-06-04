import { cacheGetter } from "@dweb-browser/helper/cacheGetter.ts";
import { shortcutPlugin } from "./shortcut.plugin.ts";

export class HTMLDwebShortcutElement extends HTMLElement {
  static readonly tagName = "dweb-shortcut";
  readonly plugin = shortcutPlugin;

  @cacheGetter()
  get registry() {
    return this.plugin.registry;
  }
}
if (!customElements.get(HTMLDwebShortcutElement.tagName)) {
  customElements.define(HTMLDwebShortcutElement.tagName, HTMLDwebShortcutElement);
}
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebShortcutElement.tagName]: HTMLDwebShortcutElement;
  }
}
