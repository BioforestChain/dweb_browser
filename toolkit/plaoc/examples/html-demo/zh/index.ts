import { configPlugin } from "@plaoc/plugins";
import "../common.ts";
import { process } from "../helper/barcode-scannering.ts";
import { message } from "../helper/debug.ts";
// 测试所有的api
const runPlugins = () => {
  message("测试 process识别图片");
  process();
};

const setLang = async () => {
  const res = await configPlugin.setLang("en", true);
  console.log("res=>", res);
};

Object.assign(globalThis, {
  setLang,
  runPlugins,
});
