import { cacheGetter } from "../../../helper/cacheGetter.ts";
import { torchPlugin } from "./torch.plugin.ts";

export class HTMLDwebTorchElement extends HTMLElement {
  static readonly tagName = "dweb-torch";
  readonly plugin = torchPlugin;

  @cacheGetter()
  get toggleTorch() {
    return torchPlugin.toggleTorch;
  }
  @cacheGetter()
  get getTorchState() {
    return torchPlugin.getTorchState;
  }
}

if (!customElements.get(HTMLDwebTorchElement.tagName)) {
  customElements.define(HTMLDwebTorchElement.tagName, HTMLDwebTorchElement);
}
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebTorchElement.tagName]: HTMLDwebTorchElement;
  }
}
