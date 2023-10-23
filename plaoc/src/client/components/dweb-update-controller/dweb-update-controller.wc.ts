import { cacheGetter } from "../../helper/cacheGetter.ts";
import { updateControllerPlugin } from "./dweb-update-controller.plugins.ts";

export class HTMLDwebUpdateControllerElement extends HTMLElement {
  static readonly tagName = "dweb-update-controller";
  readonly plugin = updateControllerPlugin;

  @cacheGetter()
  get listen() {
    return this.plugin.listen;
  }

  @cacheGetter()
  get download() {
    return this.plugin.download;
  }

  @cacheGetter()
  get pause() {
    return this.plugin.pause;
  }

  @cacheGetter()
  get resume() {
    return this.plugin.resume;
  }

  @cacheGetter()
  get cancel() {
    return this.plugin.cancel;
  }
}

customElements.define(HTMLDwebUpdateControllerElement.tagName, HTMLDwebUpdateControllerElement);
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebUpdateControllerElement.tagName]: HTMLDwebUpdateControllerElement;
  }
}
