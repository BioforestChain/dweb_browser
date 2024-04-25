import { motionSensorsPlugin } from "./motionSensors.plugin.ts";

export class HTMLDwebMotionSensorsElement extends HTMLElement {
  static readonly tagName = "dweb-motion-sensors";
  readonly plugin = motionSensorsPlugin;

  async startAccelerometer(fps?: number) {
    const controller = await motionSensorsPlugin.startAccelerometer(fps);
    controller.listen((axis) => {
      this.dispatchEvent(new CustomEvent("readAccelerometer", { detail: axis }));
    });
    return controller;
  }

  async startGyroscope(fps?: number) {
    const controller = await motionSensorsPlugin.startGyroscope(fps);
    controller.listen((axis) => {
      this.dispatchEvent(new CustomEvent("readGyroscope", { detail: axis }));
    });
    return controller
  }
}
customElements.define(HTMLDwebMotionSensorsElement.tagName, HTMLDwebMotionSensorsElement);
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebMotionSensorsElement.tagName]: HTMLDwebMotionSensorsElement;
  }
}
