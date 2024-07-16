import { cacheGetter } from "@dweb-browser/helper/cacheGetter.ts";
import { keychainPlugin } from "./keychain.plugin.ts";

export class HTMLDwebKeychainElement extends HTMLElement {
  static readonly tagName = "dweb-keychain";
  readonly plugin = keychainPlugin;

  @cacheGetter()
  get keys() {
    return this.plugin.keys;
  }
  @cacheGetter()
  get get() {
    return this.plugin.get;
  }
  @cacheGetter()
  get set() {
    return this.plugin.set;
  }
  @cacheGetter()
  get has() {
    return this.plugin.has;
  }
  @cacheGetter()
  get delete() {
    return this.plugin.delete;
  }
}

if (!customElements.get(HTMLDwebKeychainElement.tagName)) {
  customElements.define(HTMLDwebKeychainElement.tagName, HTMLDwebKeychainElement);
}
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebKeychainElement.tagName]: HTMLDwebKeychainElement;
  }
}
