import path from "node:path";
import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { getAllApps } from "../jmm/jmm.api.serve.ts"
import { HttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.ts";
import { createWWWServer } from "./browser.www.serve.ts"
import { hidden } from "https://deno.land/std@0.177.0/fmt/colors.ts";
import { createBrowserWindow } from "./browser.bw.ts"
import { createCBV } from "./browser.content.bv.ts"
import { createAddressBrowserVeiw } from "./browser.address.bv.ts"
 
export class BrowserNMM extends NativeMicroModule {
  mmid = "browser.dweb" as const;
  wwwServer: HttpDwebServer | undefined;
  apiServer: HttpDwebServer | undefined;
  protected async _bootstrap(context: $BootstrapContext) {
    // 设计思路
    // BrowserWindow 承载 BrowserView 
    // BrowserView 用来显示内容
    // BroswerView 的请求可以通过
    
  Electron.protocol.registerSchemesAsPrivileged([
    { 
      scheme: 'https', 
      privileges: { 
        standard: true,
        bypassCSP: true,
        supportFetchAPI: true,
        corsEnabled: true,
        stream: true,
      } 
    }
  ])
    const addressBarHeight = 38;
    await Electron.app.whenReady();
    const bw = createBrowserWindow()
    console.always(1)
    const contentBV = createCBV(bw, addressBarHeight)
    const addressBV = createAddressBrowserVeiw(bw, addressBarHeight)

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
    
    
     

    


    // this.bw_init();
    // this.cbv_nit()

    // Electron.protocol.registerStringProtocol("https", async (req, callback) => {
    //   const pathname = new URL(req.url).pathname;
    //   switch(pathname){
    //     case "/appsinfo":
    //       this._getAppsInfo(callback)
    //       break;
    //     default: throw new Error(`browser.ts 还有没有处理的 https 的请求 ${req.url}`)
    //   }
    // })
  }
  protected _shutdown() {}

  private async _getAppsInfo(callback: $Callback){
    const appsInfo = await getAllApps()
    callback({
      statusCode: 200,
      mimeType: "application/json",
      charset: "utf-8",
      data: JSON.stringify(appsInfo)
    })
  }

  // private _bw: Electron.BrowserWindow | undefined;
  // bw_init(){
  //   const { x, y, width, height} = this.bw_getSize()
  //   // 放到页面居中的位置
  //   const options = {
  //     x: x, 
  //     y: y,
  //     width: width,
  //     height: height,
  //     movable: true,
  //     webPreferences: {
  //       devTools: true,
  //       webSecurity: false,
  //       safeDialogs: true,
  //     },
  //   }
  //   this._bw = new Electron.BrowserWindow(options);
  //   this._bw.webContents.openDevTools()
  //   return this._bw;
  // }

  // bw_getSize(){
  //   const size = Electron.screen.getPrimaryDisplay().size
  //   const width = parseInt(size.width * 0.8 + "");
  //   const height = parseInt(size.height * 0.8 + "")
  //   const x = 0;
  //   const y = (size.height - height) / 2
  //   return {width, height, x, y}
  // }

  // bw_getTitleBarHeight(){
  //   if(this._bw === undefined) throw new Error(`this._bw === undefined`)
  //   const { height } = this.bw_getSize()
  //   const [_, contentHeight] = this._bw.getContentSize();
  //   return height - contentHeight;
  // }

  // cbv: Electron.BrowserView | undefined;
  // cbv_nit(){
  //   if(this._bw === undefined) {
  //     throw new Error("this._bw === undefined");
  //   }
  //   const index_html = path.resolve(
  //     Electron.app.getAppPath(),
  //     "assets/browser/newtab/index.html"
  //   );
  //   const options = {
  //     webPreferences: {
  //       devTools: true,
  //       webSecurity: false,
  //       safeDialogs: true,
  //     }
  //   }
  //   const [width, height] = this._bw.getContentSize();
  //   this.cbv = new Electron.BrowserView(options)
  //   this._bw.setBrowserView(this.cbv)
  //   this.cbv.setAutoResize({
  //     width: true,
  //     height: true,
  //     horizontal: true,
  //     vertical: true
  //   })
  //   this.cbv.setBounds({
  //     x: 0,
  //     y: this.bw_getTitleBarHeight(),
  //     width:  width,
  //     height: height 
  //   })
  //   this.cbv.webContents.loadFile(index_html)
  //   this.cbv.webContents.openDevTools();
  //   const session = this.cbv.webContents.session;
  //   session.webRequest.onBeforeRequest((details, callback) => {
  //     // console.always('拦截到了 请求： ', details.url, )
  //     callback({ cancel: false })
  //   })
  // }


}

 

export interface $Callback{
  (response: string | Electron.ProtocolResponse) : void
}

enum $TitleBarStyle {
  HIDDEN = "hidden",
  DEFAULT = "default",
  HIDDEN_INSET = "hiddenInset",
  CUSTOM_BUTTON_ON_HOVER = "customButtonsOnHover"
}

 
