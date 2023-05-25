import { cacheGetter } from "../../helper/cacheGetter.ts";
import { hapticsPlugin } from "./haptics.plugin.ts";

export class HTMLDwebHapticsElement extends HTMLElement {
  readonly plugin = hapticsPlugin;

  @cacheGetter()
  get vibrate() {
    return this.plugin.vibrate
  }

  @cacheGetter()
  get impactLight() {
    return this.plugin.impactLight
  }

  @cacheGetter()
  get notification() {
    return this.plugin.notification
  }

  @cacheGetter()
  get vibrateClick() {
    return this.plugin.vibrateClick
  }

  @cacheGetter()
  get vibrateDisabled() {
    return this.plugin.vibrateDisabled
  }

  @cacheGetter()
  get vibrateDoubleClick() {
    return this.plugin.vibrateDoubleClick
  }

  @cacheGetter()
  get vibrateHeavyClick() {
    return this.plugin.vibrateHeavyClick
  }

  @cacheGetter()
  get vibrateTick() {
    return this.plugin.vibrateTick
  }



}

customElements.define(hapticsPlugin.tagName, HTMLDwebHapticsElement);
