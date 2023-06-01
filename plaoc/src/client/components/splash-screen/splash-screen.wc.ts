import { cacheGetter } from "../../helper/cacheGetter.ts";
import { splashScreenPlugin } from "./splash-screen.plugin.ts";
export class HTMLDwebSplashScreenElement extends HTMLElement {
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

customElements.define(splashScreenPlugin.tagName, HTMLDwebSplashScreenElement);
