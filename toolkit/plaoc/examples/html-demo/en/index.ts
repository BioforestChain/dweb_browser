import { configPlugin, dwebServiceWorker } from "@plaoc/plugins";
import "../common.ts";

const setLang = async () => {
  const res = await configPlugin.setLang("zh");
  console.log("res=>", res);
};

Object.assign(globalThis, {
  setLang,
  dwebServiceWorker,
});
