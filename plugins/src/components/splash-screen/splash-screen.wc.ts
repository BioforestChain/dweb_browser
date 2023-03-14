import { splashScreenPlugin } from "./splash-screen.plugin.ts";

export class HTMLDwebSplashScreenElement extends HTMLElement {
  readonly plugin = splashScreenPlugin;
  get show() {
    return splashScreenPlugin.show;
  }
  get hide() {
    return splashScreenPlugin.hide;
  }
}

customElements.define(splashScreenPlugin.tagName, HTMLDwebSplashScreenElement);
