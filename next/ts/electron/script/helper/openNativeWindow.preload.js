"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.mainApis = exports.exportApis = exports.mainPort = void 0;
const dntShim = __importStar(require("../_dnt.shims.js"));
const electron_1 = require("electron");
/// 这里用了 require 函数，需要使用sandbox:false 来支持，可以通过 bundle 来把代码打包在一起
const electronRenderPort_js_1 = require("./electronRenderPort.js");
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
electron_1.ipcRenderer.postMessage("renderPort", {}, [
    export_channel.port2,
    import_channel.port2,
]);
(0, electronRenderPort_js_1.updateRenderPort)(export_port);
(0, electronRenderPort_js_1.updateRenderPort)(import_port);
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
exports.mainPort = export_port;
let apis = {};
const start = () => {
    (0, comlink_1.expose)(apis, exports.mainPort);
};
Object.assign(dntShim.dntGlobalThis, { mainPort: exports.mainPort, start });
// contextBridge.exposeInMainWorld("mainPort", renderPortToObjectPort(ipc_port));
const comlink_1 = require("comlink");
exports.exportApis = once((APIS) => {
    apis = APIS;
    start(); // 可以注释掉这行，手动启动，方便调试
});
exports.mainApis = (0, comlink_1.wrap)(import_port);
