//  file.sys.dweb 负责下载并管理安装包
const fsPromises = require("fs/promises")
const path = require('path')
const process = require('process')
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { createHttpDwebServer } from "../http-server/$createHttpDwebServer.cjs";
import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import chalk from "chalk"

import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";
import type { Ipc } from "../../core/ipc/ipc.cjs";

// 运行在 node 环境 
export class JMMMetadata extends NativeMicroModule {
    mmid = "jmmmetadata.sys.dweb" as const;
    origin = ""
    private _close_dweb_server?: () => unknown;

    async _bootstrap() {
        console.log(chalk.red('[jmm-metadata.cts _bootstramp]'))
        const {listen, close, startResult } = await createHttpDwebServer(this, {});
        this.origin = startResult.urlInfo.internal_origin
        this._close_dweb_server = close;
        ;(await listen()).onRequest(this._onRequestFromHtml)
        this._registerCommonIpcOnMessageHandlerPathRoot()
    }

    private _onRequestFromHtml = async (request: IpcRequest, ipc: Ipc) => {
        const pathname = request.parsed_url.pathname
        switch(pathname){
            case pathname === "/" || pathname === "/index.html" ? pathname : "**eot**": this._onRequestFromHtmlPathIndexHtml(request, ipc); break;
            case "/donwload": this._onRequestFromHtmlDownload(request, ipc); break;
            case "/download_process": this._onRequestFromHtmlDonwloadProcess(request, ipc); break;
            // case "/back": this._onRequestFromHtmlPathBack(request, ipc); break;

            default: this._onRequestFromHtmlDefualt(request, ipc);
        }
    }

    private _onRequestFromHtmlPathIndexHtml =  async (request: IpcRequest, ipc: Ipc) => {
        // 获取 metadata.json 文件
        const metadataUrl = request.parsed_url.searchParams.get('url')
        console.log('[jmm-metadata.cts metadataUrl:] ', metadataUrl)
        if(metadataUrl === null) {
            ipc.postMessage(
                await IpcResponse.fromText(
                  request.req_id,
                  412,
                  new IpcHeaders({
                    "Content-type": "text/plain"
                  }),
                  "确实是 查询 metadata.json 的url 参数",
                  ipc
                )
            )
            return;
        }

        const result = JSON.stringify(await (await this.nativeFetch(metadataUrl)).json())

        let content = await readHtml()
            content = content.replace("<body>", `<body><script type="text/javascript"> 
                window.metadatajsonUrl = '${metadataUrl}'; 
                window.metadataInfo = ${result};
                console.log('window', window) </script>
            `) 
        ipc.postMessage(
            await IpcResponse.fromText(
              request.req_id,
              200,
              new IpcHeaders({
                "Content-type": "text/html"
              }),
              content,
              ipc
            )
        )
    }

    // 下载
    private _onRequestFromHtmlDownload = async (request: IpcRequest, ipc: Ipc) => {
        const searchParams = request.parsed_url.searchParams
        const url = searchParams.get('url')
        const app_id = searchParams.get("app_id")
        ipc.postMessage(
            await IpcResponse.fromResponse(
                request.req_id,
                await this.nativeFetch(
                    `file://file.sys.dweb/download?url=${url}&app_id=${app_id}`,
                    {
                        method: "POST",
                        body: request.body.raw
                    }
                ),
                ipc,
            )
        )
    }

    // 下载的进度
    private _onRequestFromHtmlDonwloadProcess = async (request: IpcRequest, ipc: Ipc) => {
        const searchParams = request.parsed_url.searchParams
        const url = searchParams.get('url')
        const app_id = searchParams.get("app_id")
        ipc.postMessage(
            await IpcResponse.fromResponse(
                request.req_id,
                await this.nativeFetch(`file://file.sys.dweb/download_process?url=${url}&app_id=${app_id}`),
                ipc,
            )
        )
    }

    private _onRequestFromHtmlPathBack = async (request: IpcRequest, ipc: Ipc) => {
        
    }

    private _onRequestFromHtmlDefualt = async (request: IpcRequest, ipc: Ipc) => {
        console.log(chalk.red('jmm-metadata onRequest 出错没有匹配的处理器'), request)
        ipc.postMessage(
            await IpcResponse.fromText(
              request.req_id,
              500,
              new IpcHeaders({
                "Content-type": "text/plain"
              }),
              "没有注册匹配的监听器",
              ipc
            )
        )
        return;
    }

    private _registerCommonIpcOnMessageHandlerPathRoot = async () => {
        this.registerCommonIpcOnMessageHandler({
            pathname: "/",
            matchMode: "full",
            input: {},
            output: {},
            handler: async (args,client_ipc, request) => {
                const content = await readHtml()
                return IpcResponse.fromText(
                            request.req_id,
                            200,
                            new IpcHeaders({
                                "Content-type": "text/html"
                            }),
                            content,
                            client_ipc
                        )
                 
            },
        });
        return this;
    }


    protected _shutdown(): unknown {
        throw new Error("Method not implemented.");
    }
}

async function readHtml(){
    const result = await fsPromises.readFile(path.resolve(process.cwd(), "./assets/html/jmm-metadata.html"))
    return new TextDecoder().decode(result)
}
 