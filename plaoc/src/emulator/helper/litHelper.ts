import { css as _css, html as _html } from "lit";
// console.log("supportsAdoptingStyleSheets", supportsAdoptingStyleSheets);
export * from "lit";
export * from "lit/decorators.js";
export * from "lit/directives/class-map.js";
export * from "lit/directives/repeat.js";
export * from "lit/directives/style-map.js";
export * from "lit/directives/when.js";

/// 强制保留名称
export const css = _css;
export const html = _html;
