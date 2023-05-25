import { NativeMicroModule } from "../../../../core/micro-module.native.ts";
import { log } from "../../../../helper/devtools.ts";
import { getState, setState, startObserve, stopObserve } from "./handlers.ts";
import { ipcMain, IpcMainEvent } from "electron";
import { IpcEvent } from "../../../../core/ipc/index.ts";
import type { Ipc } from "../../../../core/ipc/ipc.ts";

export class VirtualKeyboardNMM extends NativeMicroModule {
  mmid = "virtual-keyboard.nativeui.sys.dweb" as const;
  httpIpc: Ipc | undefined;
  observes: Map<string /** headers.host */, Ipc> = new Map();
  observesState: Map<string /**headers.host */, boolean> = new Map();
  encoder = new TextEncoder();

  _bootstrap = async (context: any) => {
    log.green(`[${this.mmid} _bootstrap]`);
    {
      // 监听从 multi-webview-comp-status-bar.html.mts 通过 ipcRenderer 发送过来的 监听数据
      ipcMain.on(
        "virtual_keyboard_state_change",
        (ipcMainEvent: IpcMainEvent, host, statusbarState) => {
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
      handler: getState.bind(this),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/setState",
      matchMode: "full",
      input: {},
      output: "object",
      handler: setState.bind(this),
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
    ipc.onEvent((event: IpcEvent) => {
      if (event.name === "observe") {
        const host = event.data;
        this.observes.set(host as string, ipc);
      }
    });
  }

  _shutdown = async () => {};
}
