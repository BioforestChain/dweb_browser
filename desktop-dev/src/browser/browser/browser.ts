import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { HttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.ts";
import { createAPIServer } from "./browser.api.server.ts";
import { createBrowserWindow } from "./browser.bw.ts";
import { createCBV } from "./browser.content.bv.ts";

import type { $BW } from "./browser.bw.ts";
import type { $CBV } from "./browser.content.bv.ts";

export class BrowserNMM extends NativeMicroModule {
  mmid = "browser.dweb" as const;
  wwwServer: HttpDwebServer | undefined;
  apiServer: HttpDwebServer | undefined;
  bw: $BW | undefined;
  contentBV: $CBV | undefined;
  addressBV: Electron.BrowserView | undefined;
  // addressBVHeight = 38
  addressBVHeight = 0; // 没有地址栏

  protected async _bootstrap(context: $BootstrapContext) {
    await createAPIServer.bind(this)();
    const addressBarHeight = this._addressBarHeight();
    await Electron.app.whenReady();
    this.bw = createBrowserWindow.bind(this)();
    this.contentBV = createCBV.bind(this)(this.bw, addressBarHeight);
    // this.addressBV = createAddressBrowserVeiw.bind(this)(this.bw, addressBarHeight)
  }

  private _addressBarHeight() {
    // argv 有 --inspect === 调试状态 增加 addressBar 的高度;
    // 非调试状态选用标准高度
    // return process.argv.includes("--inspect") ? 138 : this.addressBVHeight;
    return this.addressBVHeight;
  }
  protected _shutdown() {}


}

// enum $TitleBarStyle {
//   HIDDEN = "hidden",
//   DEFAULT = "default",
//   HIDDEN_INSET = "hiddenInset",
//   CUSTOM_BUTTON_ON_HOVER = "customButtonsOnHover"
// }
