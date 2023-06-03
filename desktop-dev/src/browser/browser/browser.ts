import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { HttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.ts";
import { createBrowserWindow } from "./browser.bw.ts"
import { createCBV } from "./browser.content.bv.ts"
import { createAddressBrowserVeiw } from "./browser.address.bv.ts"
import { createAPIServer } from "./browser.api.server.ts"
 
export class BrowserNMM extends NativeMicroModule {
  mmid = "browser.dweb" as const;
  wwwServer: HttpDwebServer | undefined;
  apiServer: HttpDwebServer | undefined;
  bw: Electron.BrowserWindow | undefined;
  contentBV: Electron.BrowserView | undefined;
  addressBV: Electron.BrowserView | undefined;
  protected async _bootstrap(context: $BootstrapContext) {
    createAPIServer.bind(this)()
    
 
    const addressBarHeight = 38;
    await Electron.app.whenReady();
    this.bw = createBrowserWindow()
    this.contentBV = createCBV.bind(this)(this.bw, addressBarHeight);
    this.addressBV = createAddressBrowserVeiw.bind(this)(this.bw, addressBarHeight)

    // setTimeout(() => {
    //   console.always('重新开始载入》》》》》》》》》》》》》')
    //   try{
    //     console.always('重新载入了')
    //     contentBV.webContents.stop() 
    //     contentBV.webContents.loadURL("https://www.baidu.com")

    //     setTimeout(() => {
    //       contentBV.webContents.goBack();
    //       console.always('跳转回去了')
    //     },1000)
        
    //   }catch(err){
    //     console.always('err: ', err)
    //   }
    // }, 3000)
  }
  protected _shutdown() {}
}

// enum $TitleBarStyle {
//   HIDDEN = "hidden",
//   DEFAULT = "default",
//   HIDDEN_INSET = "hiddenInset",
//   CUSTOM_BUTTON_ON_HOVER = "customButtonsOnHover"
// }

 
