// @ts-ignore
delete Document.prototype.adoptedStyleSheets;
import { css, supportsAdoptingStyleSheets } from "lit";
console.log("supportsAdoptingStyleSheets", supportsAdoptingStyleSheets, css);
export * from "lit";
export * from "lit/decorators.js";
export * from "lit/directives/when.js";

export * from "lit/directives/class-map.js";
export * from "lit/directives/repeat.js";
export * from "lit/directives/style-map.js";
