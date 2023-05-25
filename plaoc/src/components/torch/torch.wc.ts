import { cacheGetter } from "../../helper/cacheGetter.ts";
import { torchPlugin } from "./torch.plugin.ts";

export class HTMLDwebTorchElement extends HTMLElement {
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
customElements.define(torchPlugin.tagName, HTMLDwebTorchElement);
