import path from "node:path";
import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { HttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.ts";
import { createWWWServer } from "./broser.www.serve.ts"


export class BrowserNMM extends NativeMicroModule {
  mmid = "browser.dweb" as const;
  wwwServer: HttpDwebServer | undefined;
  protected async _bootstrap(context: $BootstrapContext) {
    {
      await createWWWServer.bind(this)()
    }
    const URLInfo = this.wwwServer?.startResult.urlInfo.buildInternalUrl("/")!
    this.nativeFetch(
      `file://mwebview.browser.dweb/open?url=${URLInfo.href}`
    )

    // await Electron.app.whenReady();
    // const window = new Electron.BrowserWindow({
    //   webPreferences: {
    //     devTools: true,
    //   },
    // });
    // const index_html = path.resolve(
    //   Electron.app.getAppPath(),
    //   "assets/browser/newtab/index.html"
    // );
    // console.log("index_html", index_html);
    // // 只能够载入 mubltiwebviw
    // window.loadFile(index_html);
  }
  protected _shutdown() {}
}
