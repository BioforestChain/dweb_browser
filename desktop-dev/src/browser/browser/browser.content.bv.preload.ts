import { contextBridge, ipcRenderer } from "electron";
import { CONST } from "./const.ts"
contextBridge.exposeInMainWorld("electron", {
  messageSend: (...args: unknown[]) => ipcRenderer.send(CONST.MESSAGE_BROWSER_TO_MAIN, ...args),
  messageOn: (callback: $Callback) => ipcRenderer.on(CONST.MESSGE_BROWSER_TO_RENDER, callback)
});
 
interface $Callback{
  (...args: unknown[]): void
}
 

 
