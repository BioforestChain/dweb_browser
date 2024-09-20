import { initSync } from "npm:@dweb-browser/svg-wasm@0.7.0";

// 获取 WASM 文件的路径
const wasmPath = new URL(
  "../../../../node_modules/@dweb-browser/svg-wasm/svg_wasm_bg.wasm", // 替换为你的 WASM 文件路径
  import.meta.url
).pathname;
// 读取 WASM 文件
const svgWasmBinary = await Deno.readFile(wasmPath);
initSync(svgWasmBinary);
