import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { MICRO_MODULE_CATEGORY } from "../../core/category.const.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { $Callback, createSignal } from "../../helper/createSignal.ts";
import { tryDevUrl } from "../../helper/electronIsDev.ts";
import { createNativeWindow } from "../../helper/openNativeWindow.ts";
import { buildUrl } from "../../helper/urlHelper.ts";
import { createWWWServer } from "./server.www.ts";

export class WebBrowserNMM extends NativeMicroModule {
  mmid = "web.browser.dweb" as const;
  name = "Web Browser";
  override short_name = "Browser";
  override categories = [MICRO_MODULE_CATEGORY.Application, MICRO_MODULE_CATEGORY.Web_Browser];
  override icons: NativeMicroModule["icons"] = [
    {
      src: "file:///sys/browser/web/logo.svg",
    },
  ];

  protected async _bootstrap(context: $BootstrapContext) {
    const wwwServer = await createWWWServer.call(this);
    const host = wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/desktop.html";
    }).href;
    const browserUrl = await tryDevUrl(host, "http://localhost:3500");
    let win: Electron.BrowserWindow | undefined;
    // 激活的逻辑
    this.onActivity(async (event, ipc) => {
      if (win) {
        win.close();
        win = undefined;
        return;
      }
      // 已经被打开过了
      win = await createNativeWindow(browserUrl, {
        defaultBounds: { width: 1024, height: 700 },
      });

      this._onShutdown(() => {
        win?.close();
      });
      void win.loadURL(
        buildUrl(browserUrl, {
          search: {
            "api-base": host,
            mmid: this.mmid,
          },
        }).href
      );
    });
    //todo observer/app  openinbrowser
  }

  private _shutdown_signal = createSignal<$Callback>();
  private _onShutdown = this._shutdown_signal.listen;
  protected _shutdown() {
    this._shutdown_signal.emitAndClear();
  }
}
