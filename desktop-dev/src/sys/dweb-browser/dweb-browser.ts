// haptics.sys.dweb
import { IpcEvent } from "../../core/ipc/IpcEvent.ts"
import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { IpcResponse } from "../../core/ipc/IpcResponse.ts";
import { $IpcMessage } from "../../core/ipc/const.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { openNativeWindow } from "../../helper/openNativeWindow.ts";
import { createHttpDwebServer } from "../http-server/$createHttpDwebServer.ts";
import type { HttpServerNMM } from "../http-server/http-server.ts";
import { IpcRequest } from "../../core/ipc/IpcRequest.ts";
import { Ipc } from "../../core/ipc/index.ts";

export class DwebBrowserNMM extends NativeMicroModule {
  mmid = "dweb-browser.sys.dweb" as const;
  httpNMM: HttpServerNMM | undefined;
  impactLightStyle: $ImpactLightStyle = "HEAVY";
  notificationStyle: $NotificationStyle = "SUCCESS";

  protected async _bootstrap(context: $BootstrapContext) {
    console.always(`[${this.mmid}] _bootstrap`);
    // this.registerCommonIpcOnMessageHandler({
    //   pathname: "/activity",
    //   matchMode: "full",
    //   input: { mmid: "string" },
    //   output: "boolean",
    //   handler: async (args, ipc, request) => {
    //     const [targetIpc] = await content.dns.connect(args.mmid as $MMID)
    //     targetIpc.postMessage(
    //       IpcEvent.fromText("activity", "") as $IpcMessage
    //     )
    //     return true;
    //   },
    // });

    // this.registerCommonIpcOnMessageHandler({
    //   pathname: "/close",
    //   matchMode: "full",
    //   input: {mmid: "string"},
    //   output: "boolean",
    //   handler: async () => {
    //     return true;
    //   },
    // });

    const wwwServer = await createHttpDwebServer(this, {subdomain:"www", port:443});
    const wwwStreamIpc =await  wwwServer.listen();
    wwwStreamIpc.onRequest(async (request, ipc) => {
      let pathname = request.parsed_url.pathname
      pathname = pathname === "/" ? "index.html" : pathname;
      console.log("dwebbrowser", "接收到打了 http 请求", pathname)
      const res = await this.nativeFetch(
        "file:///sys/dweb-browser" + request.parsed_url.pathname
      )
      ipc.postMessage(
        await IpcResponse.fromResponse(
          request.req_id,
          res,
          ipc
        )
      );
    })

    const apiServer = await createHttpDwebServer(this, {subdomain:"api", port:443});
    const apiStreamIpc =await apiServer.listen();
    console.log('dwebbrowser', apiServer.startResult.urlInfo)
    apiStreamIpc.onRequest(async (request, ipc) => {
      const pathname = request.parsed_url.pathname;
      switch(pathname){
        case "/activity": this.activity(request, ipc, context);break;
        case "/close": this.close(request, ipc, context); break;
      }
    })

    const urlInfo = wwwServer.startResult.urlInfo;
    const href =  wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
    }).href
    console.log('dwebbrowser', href)


    const nww = await openNativeWindow(
      // 如果下面的 show === false 那么这个窗口是不会出现的
      wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
        url.pathname = "/index.html";
      }).href,
      {
        /// 如果起始界面是html，说明是调试模式，那么这个窗口也一同展示
        // show: true, // require.main?.filename.endsWith(".html"),
      },
      { userAgent: (userAgent) => userAgent + ` dweb-host/${urlInfo.host}` }
    );
  }

  private _allIpc = new Map<$MMID, Ipc>()
  async activity(request: IpcRequest, ipc: Ipc, context: $BootstrapContext){
    (await this.getTargetIpc(request, context)).postMessage(
      IpcEvent.fromText("activity", "")
    )
    ipc.postMessage(
      IpcResponse.fromText(
        request.req_id,
        200,
        undefined,
        "ok",
        ipc
      )
    )
  }

  async close(request: IpcRequest, ipc: Ipc, context: $BootstrapContext){
    (await this.getTargetIpc(request, context)).postMessage(
      IpcEvent.fromText("close", "")
    )
    ipc.postMessage(
      IpcResponse.fromText(
        request.req_id,
        200,
        undefined,
        "ok",
        ipc
      )
    )
  }

  async getTargetIpc(request: IpcRequest, context: $BootstrapContext){
    const searchpareams = request.parsed_url.searchParams;
    const mmid = searchpareams.get('app_id') as $MMID
    let targetIpc = this._allIpc.get(mmid)
    if(targetIpc === undefined){
      [targetIpc] = await context.dns.connect(mmid as $MMID);
      this._allIpc.set(mmid, targetIpc)
    }
    return targetIpc
  }

  protected _shutdown(): unknown {
    throw new Error("Method not implemented.");
  }
}

export type $ImpactLightStyle = "HEAVY" | "MEDIUM" | "LIGHT";
export type $NotificationStyle = "SUCCESS" | "WARNING" | "ERROR";
