//  app.sys.dweb 负责启动 第三方应用程序
 
import fsPromises from "node:fs/promises"
import path from "node:path"
import process from "node:process"
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { createHttpDwebServer } from "../http-server/$createHttpDwebServer.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs"
import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import type { $AppInfo } from "../file/file-get-all-apps.cjs";
import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs"
import chalk from "chalk";

// 运行在 node 环境 
export class BrowserNMM extends NativeMicroModule {
    mmid = "browser.sys.dweb" as const;    
    private _close_dweb_server?: () => unknown;

    async _bootstrap() {
     
      const {listen, close, startResult } = await createHttpDwebServer(this, {});
      this._close_dweb_server = close;
      ;(await listen()).onRequest(async (request, ipc) => {
        const pathname = request.parsed_url.pathname;
        switch(pathname){
          case pathname === "/" || pathname === "/index" ? pathname : "**eot**":
            this._onRequestPathIndexHtml(request, ipc);
            break;
          case "/open_webview":
            this._onRequestPathOpenWebview(request, ipc);
            break;
          case "/appsinfo": 
            this._onRequestPathNameAppsInfo(request, ipc); 
            break;
          case `/install`: 
            this._onRequestPathNameInstall(request, ipc); 
            break;
          case `/open`:
            this._onRequestPathNameOpen(request, ipc); 
            break;
          default:
            this._onRequestDefault(request, ipc);
            break;
        }
      })
        
      //  安装 第三方 app
      this.registerCommonIpcOnMessageHandler({
          pathname: "/",
          matchMode: "full",
          input: {},
          output: "boolean",
          handler: async (args,client_ipc, request) => {
            console.log(chalk.green('browser.sys.dweb 接收到奥了消息 registerCommonIpcOnMessageHandler /'))
              
              return true;
          },
      });;

      this.fetch(`file://mwebview.sys.dweb/open?url=${encodeURIComponent(startResult.urlInfo.internal_origin)}`).text();
        
    }

    protected _shutdown(): unknown {
        throw new Error("Method not implemented.");
    }
    // _shutdown() {}
    // private register(mmid: $MMID) {
    //   /// TODO 这里应该有用户授权，允许开机启动
    // //   this.registeredMmids.add(mmid);
    //   return true;
    // }
    // private unregister(mmid: $MMID) {
    //   /// TODO 这里应该有用户授权，取消开机启动
    //   return this.registeredMmids.delete(mmid);
    // }
    // $Routers: {
    //    "/register": IO<mmid, boolean>;
    //    "/unregister": IO<mmid, boolean>;
    // };

    _onRequestPathIndexHtml = async (request:IpcRequest, ipc: Ipc) => {
      let content = await fsPromises
                    .readFile(
                      path.resolve(process.cwd(), "./assets/html/browser.html")
                    )
      ipc.postMessage(
        await IpcResponse.fromText(
          request.req_id,
          200,
          new IpcHeaders({
            "Content-type": "text/html",
          }),
          new TextDecoder().decode(content),
          ipc
        )
      )
    }

    _onRequestPathOpenWebview = async (request:IpcRequest, ipc: Ipc) => {
      const mmid = request.parsed_url.searchParams.get('mmid')
      console.log(chalk.green('browser.sys.dweb 接收到奥了消息 _onRequestPathOpenWebview', mmid))
      this.fetch(`file://dns.sys.dweb/open?app_id=${mmid}`)
      .then(async (res: any) => {
        ipc.postMessage(
          await IpcResponse.fromResponse(
            request.req_id,
            res,
            ipc
          )
        );
      })
      .catch((err: any) => console.log('err:', err))
    }

    _onRequestPathNameAppsInfo = async (request:IpcRequest, ipc: Ipc) => {
      const url = `file://file.sys.dweb/appsinfo`;
      this.fetch(url)
        .then(async (res: Response) => {
          // 转发给 html
          ipc.postMessage(
            await IpcResponse.fromResponse(request.req_id, res, ipc)
          );
        })
        .catch((err) => {
          console.log("获取全部的 appsInfo 失败： ", err);
        });
    }

    _onRequestPathNameInstall = async (request:IpcRequest, ipc: Ipc) => {
      const _url = `file://app.sys.dweb${request.url}`;
      this.
      fetch(_url).then(async (res: Response) => {
        ipc.postMessage(
          await IpcResponse.fromResponse(request.req_id, res, ipc)
        );
      });

    }

    _onRequestPathNameOpen = async (request:IpcRequest, ipc: Ipc) => {
      const _url = `file://app.sys.dweb${request.url}`;
      this.
      fetch(_url).then(async (res: Response) => {
        ipc.postMessage(
          await IpcResponse.fromResponse(request.req_id, res, ipc)
        );
      });
    }

    _onRequestDefault = async (request:IpcRequest, ipc: Ipc) => {

    }
}

 