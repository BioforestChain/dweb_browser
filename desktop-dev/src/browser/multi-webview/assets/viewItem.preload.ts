declare const require: (id: "electron") => typeof import("electron");

export default function preload() {
  try {
    const { contextBridge, ipcRenderer } = require("electron");

    // 这里需要修改 registryToken， tryClose 都是只发送指令
    // 实际上执行的方法还是在 服务里面
    contextBridge.exposeInMainWorld("__native_close_watcher_kit__", {
      _watchers:
        new Map() /** 没有会报错 __native_close_watcher_keit__ 传递过去后会被冻结*/,
      _tasks:
        new Map() /** 没有会报错 __native_close_watcher_keit__ 传递过去后会被冻结*/,
      registryToken(consumeToken: string) {
        if (consumeToken === null || consumeToken === "") {
          throw new Error("CloseWatcher.registryToken invalid arguments");
        }
        // 好像这个token 还没有用处的
        ipcRenderer.sendToHost("__native_close_watcher_kit__", {
          action: "registry_token",
          value: consumeToken,
        });
        // cset.add(consumeToken)
      },
      tryClose(id: string) {
        // 把消息发送给 html
        ipcRenderer.sendToHost("__native_close_watcher_kit__", {
          action: "close",
          value: id,
        });
        // watcher.dispatchEvent(new Event("close"));
      },
    });

    console.log("%cpreload success", "color:green");
  } catch (error) {
    console.error("preload fail:", error);
  }
}
