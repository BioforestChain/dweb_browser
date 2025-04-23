import { initSync } from "npm:@dweb-browser/svg-wasm";
import { node_fs, node_path, node_process, node_url } from "../deps/node.ts";
import { findDependencyPath } from "../helper/find-dependency.ts";

const dependencyPath = findDependencyPath(import.meta.dirname ?? node_process.cwd(), "@dweb-browser/svg-wasm");

const svgWasmPath = dependencyPath
  ? node_path.join(dependencyPath, "svg_wasm_bg.wasm")
  : node_url.fileURLToPath(import.meta.resolve("../../node_modules/@dweb-browser/svg-wasm/svg_wasm_bg.wasm"));

const svg_wasm_binary = node_fs.readFileSync(svgWasmPath);

initSync({ module: svg_wasm_binary });
