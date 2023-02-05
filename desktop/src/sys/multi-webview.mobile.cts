import { NativeMicroModule } from "../core/micro-module.native.cjs";
import { openNwWindow } from "../helper/openNwWindow.cjs";

// @ts-ignore
type $APIS = typeof import("./multi-webview.html.mjs")["APIS"];
/**
 * 构建一个视图树
 * 如果是桌面版，所以不用去管树的概念，直接生成生成就行了
 * 但这里是模拟手机版，所以还是构建一个层级视图
 */
export class MultiWebviewNMM extends NativeMicroModule {
  mmid = "mwebview.sys.dweb" as const;
  private window?: nw.Window;
  async _bootstrap() {
    const window = await openNwWindow("../../multi-webview.html", {
      id: "multi-webview",
      show_in_taskbar: true,
    });
    if (window.window.APIS_READY !== true) {
      await new Promise((resolve) => {
        window.window.addEventListener("apis-ready", resolve);
      });
    }
    const APIS = window.window as $APIS;
    this.registerCommonIpcOnMessageHanlder({
      pathname: "/open",
      matchMode: "full",
      input: { url: "string" },
      output: "number",
      hanlder: (args, client_ipc) => {
        console.log("open webview:", args.url);
        return APIS.openWebview(client_ipc.uid, args.url);
      },
    });
    this.window = window;
  }
  _shutdown() {
    this.window?.close();
    this.window = undefined;
  }
}
