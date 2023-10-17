import { motionSensorsPlugin } from "./motionSensors.plugin.ts";

export class HTMLDwebMotionSensorsElement extends HTMLElement {
  static readonly tagName = "dweb-motion-sensors";
  readonly plugin = motionSensorsPlugin;

  startAccelerometer() {
    motionSensorsPlugin.startAccelerometer();
    motionSensorsPlugin.onAccelerometer((axis) => {
      this.dispatchEvent(new CustomEvent("readAccelerometer", { detail: axis }));
    });
  }

  startGyroscope() {
    motionSensorsPlugin.startGyroscope();
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
