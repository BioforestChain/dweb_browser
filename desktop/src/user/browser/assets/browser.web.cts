/// <reference lib="DOM"/>
import type { IpcRequest } from "../../../core/ipc/IpcRequest.cjs";
const txt = require("../../../../bundle/browser.render.txt")
 
export const CODE = async (require: IpcRequest) => {
  return txt
};

 

