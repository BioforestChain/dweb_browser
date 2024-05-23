import "./common.ts";
import { process } from "./helper/barcode-scannering.ts";
import { message } from "./helper/debug.ts";
// 测试所有的api
const runPlugins = () => {
  message("测试 process识别图片");
  process();
};

Object.assign(globalThis, {
  runPlugins,
});
