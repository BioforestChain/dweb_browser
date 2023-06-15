import { cacheGetter } from "../../helper/cacheGetter.ts";
import { HTMLStateObserverElement } from "../../util/HTMLStateObserverElement.ts";
import { navigationBarPlugin } from "./navigation-bar.plugin.ts";
import {
  $NavigationBarRawState,
  $NavigationBarState,
} from "./navigation-bar.type.ts";

export class HTMLDwebNavigationBarElement extends HTMLStateObserverElement<
  $NavigationBarRawState,
  $NavigationBarState
> {
  static readonly tagName = "dweb-navigation-bar";
  readonly plugin = navigationBarPlugin;
  constructor() {
    super(navigationBarPlugin.state);
  }
  @cacheGetter()
  get setColor() {
    return navigationBarPlugin.setColor;
  }
  @cacheGetter()
  get getColor() {
    return navigationBarPlugin.getColor;
  }
  @cacheGetter()
  get setStyle() {
    return navigationBarPlugin.setStyle;
  }
  @cacheGetter()
  get getStyle() {
    return navigationBarPlugin.getStyle;
  }
  @cacheGetter()
  get show() {
    return navigationBarPlugin.show;
  }
  @cacheGetter()
  get hide() {
    return navigationBarPlugin.hide;
  }
  @cacheGetter()
  get setVisible() {
    return navigationBarPlugin.setVisible;
  }
  @cacheGetter()
  get getVisible() {
    return navigationBarPlugin.getVisible;
  }
  @cacheGetter()
  get getState() {
    return navigationBarPlugin.getState;
  }
  @cacheGetter()
  get setOverlay() {
    return navigationBarPlugin.setOverlay;
  }
  @cacheGetter()
  get getOverlay() {
    return navigationBarPlugin.getOverlay;
  }
}

customElements.define(
  HTMLDwebNavigationBarElement.tagName,
  HTMLDwebNavigationBarElement
);

declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebNavigationBarElement.tagName]: HTMLDwebNavigationBarElement;
  }
}
