import { torchPlugin } from "./torch.plugin.ts";

export class HTMLDwebTorchElement extends HTMLElement {
  readonly plugin = torchPlugin;

  get toggleTorch() {
    return torchPlugin.toggleTorch;
  }
  get getTorchState() {
    return torchPlugin.getTorchState;
  }
}
customElements.define(torchPlugin.tagName, HTMLDwebTorchElement);
