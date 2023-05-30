// 模拟状态栏模块-用来提供状态UI的模块
import { ipcMain, IpcMainEvent } from "electron";
import type { Ipc } from "../../../../core/ipc/ipc.ts";
import { IpcEvent } from "../../../../core/ipc/IpcEvent.ts";
import { NativeMicroModule } from "../../../../core/micro-module.native.ts";
import {
  barGetState,
  barSetState,
} from "../../../multi-webview/multi-webview.mobile.handler.ts";
import { startObserve, stopObserve } from "./handlers.ts";

export class NavigationBarNMM extends NativeMicroModule {
  mmid = "navigation-bar.nativeui.sys.dweb" as const;
  httpIpc: Ipc | undefined;
  observes: Map<string /** headers.host */, Ipc> = new Map();
  observesState: Map<string /**headers.host */, boolean> = new Map();
  encoder = new TextEncoder();

  _bootstrap = () => {
    console.always(`[${this.mmid} _bootstrap]`);

    {
      // 监听从 multi-webview-comp-status-bar.html.mts 通过 ipcRenderer 发送过来的 监听数据
      ipcMain.on(
        "navigation_bar_state_change",
        (
          _: IpcMainEvent,
          host: string,
          statusbarState: { [key: string]: unknown }
        ) => {
          const b = this.observesState.get(host);
          if (b === true) {
            const ipc = this.observes.get(host);
            if (ipc === undefined) throw new Error(`ipc === undefined`);
            ipc.postMessage(
              IpcEvent.fromText("observe", `${JSON.stringify(statusbarState)}`)
            );
          }
        }
      );
    }

    this.registerCommonIpcOnMessageHandler({
      pathname: "/getState",
      matchMode: "full",
      input: {},
      output: "object",
      handler: barGetState.bind(this, "navigationBarGetState"),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/setState",
      matchMode: "full",
      input: {},
      output: "object",
      handler: barSetState.bind(this, "navigationBarSetState"),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/startObserve",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: startObserve.bind(this),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/stopObserve",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: stopObserve.bind(this),
    });
  };

  override _onConnect(ipc: Ipc) {
    this.observes.set(ipc.remote.mmid, ipc);
    ipc.onClose(() => {
      this.observes.delete(ipc.remote.mmid)
      this.observesState.delete(ipc.remote.mmid)
    })
  }

  _shutdown = () => {
    throw new Error("[error:]还没有写关闭程序");
  };
}
