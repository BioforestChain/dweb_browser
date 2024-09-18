import fs from "node:fs";
import url from "node:url";
import { initSync } from "npm:@dweb-browser/svg-wasm";

const svg_wasm_binary = fs.readFileSync(
  url.fileURLToPath(import.meta.resolve("../../node_modules/@dweb-browser/svg-wasm/svg_wasm_bg.wasm"))
);

initSync(svg_wasm_binary);
