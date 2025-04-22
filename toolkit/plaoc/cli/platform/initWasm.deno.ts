import { initSync } from "npm:@dweb-browser/svg-wasm";
import { node_path } from "../deps/node.ts";
import { findDependencyPath } from "../helper/find-dependency.ts";

// 获取 WASM 文件的路径
const dependencyPath = findDependencyPath(Deno.cwd(), "@dweb-browser/svg-wasm");

const wasmPath = dependencyPath
  ? node_path.join(dependencyPath, "svg_wasm_bg.wasm")
  : new URL(
      "../../../../node_modules/@dweb-browser/svg-wasm/svg_wasm_bg.wasm", // 替换为你的 WASM 文件路径
      import.meta.url
    ).pathname;
// 读取 WASM 文件
const svgWasmBinary = await Deno.readFile(wasmPath);
initSync(svgWasmBinary);
