import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { MICRO_MODULE_CATEGORY } from "../../core/category.const.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { $Callback, createSignal } from "../../helper/createSignal.ts";
import { tryDevUrl } from "../../helper/electronIsDev.ts";
import { createNativeWindow } from "../../helper/openNativeWindow.ts";
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
    const win = await createNativeWindow(
      await tryDevUrl(
        wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
          url.pathname = "/index.html";
        }).href,
        "http://localhost:3500"
      ),
      { defaultBounds: { width: 1024, height: 700 } }
    );
    this._onShutdown(() => {
      win.close();
    });
  }

  private _shutdown_signal = createSignal<$Callback>();
  private _onShutdown = this._shutdown_signal.listen;
  protected _shutdown() {
    this._shutdown_signal.emitAndClear();
  }
}
