import { safeAreaPlugin } from "./safe-area.plugin.ts";

export class HTMLDwebSafeAreaElement extends HTMLElement {}
customElements.define(safeAreaPlugin.tagName, HTMLDwebSafeAreaElement);
