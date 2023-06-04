import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { HttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.ts";
import { createBrowserWindow } from "./browser.bw.ts"
import { createCBV } from "./browser.content.bv.ts"
import { createAddressBrowserVeiw } from "./browser.address.bv.ts"
import { createAPIServer } from "./browser.api.server.ts"
import type { $CBV } from "./browser.content.bv.ts";
 
export class BrowserNMM extends NativeMicroModule {
  mmid = "browser.dweb" as const;
  wwwServer: HttpDwebServer | undefined;
  apiServer: HttpDwebServer | undefined;
  bw: Electron.BrowserWindow | undefined;
  contentBV: $CBV | undefined;
  addressBV: Electron.BrowserView | undefined;
  
  protected async _bootstrap(context: $BootstrapContext) {
    await createAPIServer.bind(this)()
    const addressBarHeight = 138;
    await Electron.app.whenReady();
    this.bw = createBrowserWindow.bind(this)()
    this.contentBV = createCBV.bind(this)(this.bw, addressBarHeight);
    this.addressBV = createAddressBrowserVeiw.bind(this)(this.bw, addressBarHeight)
  }
  protected _shutdown() {}
}

// enum $TitleBarStyle {
//   HIDDEN = "hidden",
//   DEFAULT = "default",
//   HIDDEN_INSET = "hiddenInset",
//   CUSTOM_BUTTON_ON_HOVER = "customButtonsOnHover"
// }

 
