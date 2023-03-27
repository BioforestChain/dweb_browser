import path from "node:path"
import fsPromises from "node:fs/promises";
import { log } from "../../../helper/devtools.cjs";
import { IPC_MESSAGE_TYPE } from "../../../core/ipc/const.cjs"
import { createHttpDwebServer } from "../../http-server/$createHttpDwebServer.cjs";
import { IpcHeaders } from "../../../core/ipc/IpcHeaders.cjs";
import { IpcResponse } from "../../../core/ipc/IpcResponse.cjs";
import type{ NativeMicroModule } from "../../../core/micro-module.native.cjs";
import type { HttpDwebServer } from "../../http-server/$createHttpDwebServer.cjs"
import type { $IpcMessage  } from "../../../core/ipc/const.cjs";
import type { IpcRequest } from "../../../core/ipc/IpcRequest.cjs";
import type { Ipc } from "../../../core/ipc/ipc.cjs"
import type { 
    $StatusbarPluginsRequestMap,
} from "./status-bar.main.cjs"
 
export class WWWServer{
    server: HttpDwebServer | undefined;
    constructor(
        readonly nmm: NativeMicroModule,
        readonly statusbarPluginsRequestMap: $StatusbarPluginsRequestMap, 
    ){
       this._int()
    }

    private _int = async () => {
        this.server = await createHttpDwebServer(this.nmm, {});
        log.green(`[${this.nmm.mmid}] ${this.server.startResult.urlInfo.internal_origin}`);
        (await this.server.listen()).onMessage(this._onMessage)
    }

    private _onMessage = (message: $IpcMessage , ipc: Ipc) => {
        switch(message.type){
            case IPC_MESSAGE_TYPE.REQUEST: 
                this._onRequest(message, ipc);
                break;
        }
    }

    private _onRequest = (request: IpcRequest , ipc: Ipc) => {
        const pathname = request.parsed_url.pathname;
        switch(pathname){
            case "/" || "/index.html":
                this._onRequestIndex(request, ipc);
                break;
        }
    }

    private _onRequestIndex = async (request: IpcRequest , ipc: Ipc) => {
        ipc.postMessage(
            await IpcResponse.fromText(
                request.req_id,
                200,
                new IpcHeaders({
                "Content-type": "text/html",
                }),
                await reqadHtmlFile(),
                ipc
            )
        );
        return this;
    }

    private _onRequestOperationReturn = async (request: IpcRequest , ipc: Ipc) => {
        const id = request.headers.get("id");
        const appUrlFromStatusbarHtml =
          request.parsed_url.searchParams.get("app_url");
        if (!id) {
            return this._noId(request.req_id, ipc)
        }

        if (appUrlFromStatusbarHtml === null) {
            return this._noAppUrl(request.req_id, ipc)
        }
        
        let statusbarPluginRequestArry =
          this.statusbarPluginsRequestMap.get(
            appUrlFromStatusbarHtml
          );

        if(statusbarPluginRequestArry === undefined){
            throw new Error('statusbarPluginRequestArry === undefined')
        }

        let itemIndex = 
          statusbarPluginRequestArry.findIndex(
            (_item) => _item.id === id
          );

        if(itemIndex === -1){
          throw new Error(`[status-bar.main.cts statusbarPluginRequestArry 没有发现匹配的 item]`)
        }  

        let item = statusbarPluginRequestArry[itemIndex];
        statusbarPluginRequestArry.splice(itemIndex, 1);

        item.callback(
          IpcResponse.fromStream(
            item.req_id,
            200,
            request.headers,
            await request.body.stream(),
            ipc,
          )
        )

        // 返回 /operation_return 的请求
        ipc.postMessage(
          await IpcResponse.fromText(
            request.req_id,
            200,
            new IpcHeaders({
              "Content-type": "text/plain",
            }),
            "ok",
            ipc
          )
        );
    }

    private _noAppUrl = async (
        req_id: number,
        ipc: Ipc
    ) => {
        /**已经测试走过了 */
        ipc.postMessage(
            IpcResponse.fromText(
                req_id,
                400,
                new IpcHeaders({
                "Content-type": "text/plain",
                }),
                "缺少 app_url 查询参数",
                ipc
            )
        )
    } 

    private _noId = async (
        req_id: number,
        ipc: Ipc
    ) => {
        /**已经测试走过了 */
        ipc.postMessage(
            IpcResponse.fromText(
                req_id,
                400,
                new IpcHeaders({
                "Content-type": "text/plain",
                }),
                "headers 缺少了 id 标识符",
                ipc
            )
        )
    } 

}

// 读取 html 文件
async function reqadHtmlFile(){
    const targetPath = path.resolve(
      process.cwd(),
      "./assets/html/status-bar.html"
    );
    const content = await fsPromises.readFile(targetPath)
    return new TextDecoder().decode(content)
}