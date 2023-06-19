import { ipcRenderer } from "electron";
import { updateRenderPort } from "./electronRenderPort.ts";
const once = <F extends (...args: any[]) => any>(fn: F) => {
  let runed = false;
  let result: any;
  return ((...args: any[]) => {
    if (runed === false) {
      runed = true;
      result = fn(...args);
    }
    return result;
  }) as F;
};

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

const wm_listener = new Map<$MessageEventListener, $MessageEventListener>();
type $MessageEventListener = (message: MessageEvent) => void;
const resolveListener = (listener: $MessageEventListener) => {
  let resolved_listener = wm_listener.get(listener);
  if (resolved_listener === undefined) {
    resolved_listener = (event) => {
      listener({
        type: event.type,
        data: event.data,
        ports: event.ports.map(renderPortToObjectPort),
      } as any);
    };
    /// 双向绑定
    wm_listener.set(listener, resolved_listener);
    wm_listener.set(resolved_listener, listener);
  }
  return resolved_listener;
};

const renderPortToObjectPort = <T extends MessagePort>(port: T) => {
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

let apis: unknown = {};
const start = () => {
  expose(apis, mainPort);
};
Object.assign(globalThis, { mainPort, start });

// contextBridge.exposeInMainWorld("mainPort", renderPortToObjectPort(ipc_port));

import { expose, wrap } from "comlink";

export const exportApis = once((APIS: unknown) => {
  apis = APIS;
  start(); // 可以注释掉这行，手动启动，方便调试
});

export const mainApis =
  wrap<import("./openNativeWindow.ts").ForRenderApi>(import_port);
