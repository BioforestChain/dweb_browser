import { cacheGetter } from "@dweb-browser/helper/cacheGetter.ts";
import { splashScreenPlugin } from "./splash-screen.plugin.ts";
export class HTMLDwebSplashScreenElement extends HTMLElement {
  static readonly tagName = "dweb-splash-screen";
  readonly plugin = splashScreenPlugin;

  @cacheGetter()
  get show() {
    return splashScreenPlugin.show;
  }

  @cacheGetter()
  get hide() {
    return splashScreenPlugin.hide;
  }
}

if (!customElements.get(HTMLDwebSplashScreenElement.tagName)) {
  customElements.define(HTMLDwebSplashScreenElement.tagName, HTMLDwebSplashScreenElement);
}
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebSplashScreenElement.tagName]: HTMLDwebSplashScreenElement;
  }
}
