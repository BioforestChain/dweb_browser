import { contextBridge, ipcRenderer } from "electron";
contextBridge.exposeInMainWorld("electron", {
  ipcRenderer: ipcRenderer,
});

// const cset = new Set<string>();

// 这里需要修改 registryToken， tryClose 都是只发送指令
// 实际上执行的方法还是在 服务里面
contextBridge.exposeInMainWorld("__native_close_watcher_kit__", {
  _watchers: new Map(), /** 没有会报错 __native_close_watcher_keit__ 传递过去后会被冻结*/
  _tasks: new Map(), /** 没有会报错 __native_close_watcher_keit__ 传递过去后会被冻结*/
  registryToken(consumeToken: string) {
    if (consumeToken === null || consumeToken === "") {
      throw new Error("CloseWatcher.registryToken invalid arguments");
    }
    // 好像这个token 还没有用处的
    ipcRenderer.sendToHost('__native_close_watcher_kit__',{action: "registry_token", value: consumeToken})
    // cset.add(consumeToken)
  },
  tryClose(id: string) {
    // 把消息发送给 html
    ipcRenderer.sendToHost('__native_close_watcher_kit__',{action: "close", value: id})
    // watcher.dispatchEvent(new Event("close"));
  },
});

// contextBridge.exposeInMainWorld("__native_close_watcher_kit__", {
//   registryToken(consumeToken: string) {
//     if (consumeToken === null || consumeToken === "") {
//       throw new Error("CloseWatcher.registryToken invalid arguments");
//     }
//     console.log("this._tasks: ", this)
//     const resolve = this._tasks.get(consumeToken);
//     if (resolve === undefined) throw new Error("resolve === undefined");
//     const id = this.allc++;
//     resolve(id + "");
//   },
//   tryClose(id: string) {
//     const watcher = this._watchers.get(id);
//     if (watcher === undefined) throw new Error("watcher === undefined");
//     watcher.dispatchEvent(new Event("close"));
//   },
// });

// globalThis 和 window 指向的都是当前preload 里面的全局对象，和载入页面中的全局对象不是同一个；
// 同时 这里的 this 指向的是函数本身
// 雷士函数这样的原型是不会被继承的


// declare namespace window{
//   let __native_close_watcher_kit__: $__native_close_watcher_kit__;
// }

// export interface $__native_close_watcher_kit__{
//   allc: number;
//   /**
//    * 该对象由 web 侧负责写入，由 native 侧去触发事件
//    */
//   _watchers: Map<string, EventTarget>;
//   /**
//   * 该对象由 web 侧负责写入，由 native 侧去调用
//   */
//   _tasks: Map<string, (id: string) => void>;
// }
 