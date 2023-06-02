import path from "node:path";
import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";

export class BrowserNMM extends NativeMicroModule {
  mmid = "browser.dweb" as const;
  protected async _bootstrap(context: $BootstrapContext) {
    await Electron.app.whenReady();

    const size = Electron.screen.getPrimaryDisplay().size
    const window = new Electron.BrowserWindow({
      width: size.width,
      height: size.height,
      webPreferences: {
        devTools: true,
      },
    });
    const index_html = path.resolve(
      Electron.app.getAppPath(),
      "assets/browser/newtab/index.html"
    );
    console.log("index_html", index_html);
      
    // 只能够载入 mubltiwebviw
    window.loadFile(index_html);
    window.webContents.openDevTools()
  }
  protected _shutdown() {}
}
