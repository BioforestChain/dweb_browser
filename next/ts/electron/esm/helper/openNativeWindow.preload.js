import * as dntShim from "../_dnt.shims.js";
import { ipcRenderer } from "electron";
/// 这里用了 require 函数，需要使用sandbox:false 来支持，可以通过 bundle 来把代码打包在一起
import { updateRenderPort } from "./electronRenderPort.js";
const once = (fn) => {
    let runed = false;
    let result;
    return ((...args) => {
        if (runed === false) {
            runed = true;
            result = fn(...args);
        }
        return result;
    });
};
// ipcRenderer.on("renderPort", (event) => {
//   const port = event.ports[0];
//   //   port.addEventListener("message", (event) => {
//   //     console.log("prepare on messaage", event.data);
//   //   });
//   //   setInterval(() => {
//   //     console.log("prepare send 2");
//   //     port.postMessage(2);
//   //   }, 1000);
//   //   port.start();
// });
/**
 * port1 给内部使用
 * port2 给外部
 */
const export_channel = new MessageChannel();
const import_channel = new MessageChannel();
const export_port = export_channel.port1;
const import_port = import_channel.port1;
ipcRenderer.postMessage("renderPort", {}, [
    export_channel.port2,
    import_channel.port2,
]);
updateRenderPort(export_port);
updateRenderPort(import_port);
// setInterval(() => {
//   console.log("render send msg", 1);
//   ipc_port.postMessage(1);
// }, 1000);
// ipc_port.addEventListener("message", (event) => {
//   console.log("render recv msg", event.data);
// });
const wm_listener = new Map();
const resolveListener = (listener) => {
    let resolved_listener = wm_listener.get(listener);
    if (resolved_listener === undefined) {
        resolved_listener = (event) => {
            listener({
                type: event.type,
                data: event.data,
                ports: event.ports.map(renderPortToObjectPort),
            });
        };
        /// 双向绑定
        wm_listener.set(listener, resolved_listener);
        wm_listener.set(resolved_listener, listener);
    }
    return resolved_listener;
};
const renderPortToObjectPort = (port) => {
    return port;
    // const addEventListener = port.addEventListener.bind(port);
    // const removeEventListener = port.removeEventListener.bind(port);
    // return Object.assign(port, {
    //   start: port.start.bind(port),
    //   addEventListener: (type: "message", listener: $MessageEventListener) => {
    //     addEventListener(type, resolveListener(listener));
    //   },
    //   removeEventListener: (type: "message", listener: $MessageEventListener) => {
    //     removeEventListener(type, resolveListener(listener));
    //   },
    //   postMessage: port.postMessage.bind(port),
    //   close: port.close.bind(port),
    // }) as T;
};
export const mainPort = export_port;
let apis = {};
const start = () => {
    expose(apis, mainPort);
};
Object.assign(dntShim.dntGlobalThis, { mainPort, start });
// contextBridge.exposeInMainWorld("mainPort", renderPortToObjectPort(ipc_port));
import { expose, wrap } from "comlink";
export const exportApis = once((APIS) => {
    apis = APIS;
    start(); // 可以注释掉这行，手动启动，方便调试
});
export const mainApis = wrap(import_port);
