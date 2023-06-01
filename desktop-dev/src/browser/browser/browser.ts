import path from "node:path";
import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";

export class BrowserNMM extends NativeMicroModule {
  mmid = "browser.dweb" as const;
  protected async _bootstrap(context: $BootstrapContext) {
    await Electron.app.whenReady();
    const window = new Electron.BrowserWindow({
      webPreferences: {
        devTools: true,
      },
    });
    const index_html = path.resolve(
      Electron.app.getAppPath(),
      "assets/browser/newtab/index.html"
    );
    console.log("index_html", index_html);
    window.loadFile(index_html);
  }
  protected _shutdown() {}
}
