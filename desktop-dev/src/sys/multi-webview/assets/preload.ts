import { contextBridge, ipcRenderer } from "electron";
contextBridge.exposeInMainWorld("electron", {
  ipcRenderer: ipcRenderer,
});

contextBridge.exposeInMainWorld("__native_close_watcher_kit__", {
  allc: 0,
  _watchers: new Map(),
  _tasks: new Map(),
  registryToken: function (consumeToken: string) {
    if (consumeToken === null || consumeToken === "") {
      throw new Error("CloseWatcher.registryToken invalid arguments");
    }
    const resolve = this._tasks.get(consumeToken);
    if (resolve === undefined) throw new Error("resolve === undefined");
    const id = this.allc++;
    resolve(id + "");
  },
  tryClose: function (id: string) {
    const watcher = this._watchers.get(id);
    if (watcher === undefined) throw new Error("watcher === undefined");
    watcher.dispatchEvent(new Event("close"));
  },
});