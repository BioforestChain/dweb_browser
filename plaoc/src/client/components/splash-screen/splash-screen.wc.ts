import { cacheGetter } from "../../helper/cacheGetter.ts";
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

customElements.define(HTMLDwebSplashScreenElement.tagName, HTMLDwebSplashScreenElement);
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebSplashScreenElement.tagName]: HTMLDwebSplashScreenElement;
  }
}
