import { motionSensorsPlugin } from "./motionSensors.plugin.ts";

export class HTMLDwebMotionSensorsElement extends HTMLElement {
  static readonly tagName = "dweb-motion-sensors";
  readonly plugin = motionSensorsPlugin;

  startAccelerometer(interval?: number) {
    motionSensorsPlugin.startAccelerometer(interval);
    motionSensorsPlugin.onAccelerometer((axis) => {
      this.dispatchEvent(new CustomEvent("readAccelerometer", { detail: axis }));
    });
  }

  startGyroscope(interval?: number) {
    motionSensorsPlugin.startGyroscope(interval);
    motionSensorsPlugin.onGyroscope((axis) => {
      this.dispatchEvent(new CustomEvent("readGyroscope", { detail: axis }));
    });
  }
}
customElements.define(HTMLDwebMotionSensorsElement.tagName, HTMLDwebMotionSensorsElement);
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebMotionSensorsElement.tagName]: HTMLDwebMotionSensorsElement;
  }
}
