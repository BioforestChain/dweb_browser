
import type { IpcRequest } from "../../../core/ipc/IpcRequest.cjs";

/// <reference lib="DOM"/>
const script = () => {
    console.log('进入 browser.web.mjs.cts')
};

export const CODE = async (require: IpcRequest) => {
  return script.toString().match(/\{([\w\W]+)\}/)![1];
};

