//  file.sys.dweb 负责下载并管理安装包
const fsPromises = require("fs/promises")
const path = require('path')
const process = require('process')
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { createHttpDwebServer } from "../http-server/$listenHelper.cjs";
 
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";
import type { Ipc } from "../../core/ipc/ipc.cjs";




// 运行在 node 环境 
export class FileNMM extends NativeMicroModule {
    mmid = "jmmMetadata.sys.dweb" as const;
    private _close_dweb_server?: () => unknown;

    async _bootstrap() {
        const { origin, listen, close } = await createHttpDwebServer(this, {});
        this._close_dweb_server = close;
        ;(await listen()).onRequest(this._onRequestFromHtml)
        this._registerCommonIpcOnMessageHandlerPathRoot()
    }

    private _onRequestFromHtml = async (request: IpcRequest, ipc: Ipc) => {
        const pathname = request.parsed_url.pathname
        switch(pathname){
            case pathname === "/" || pathname === "/index.html" ? pathname : "**eot**": this._onRequestFromHtmlPathIndexHtml(request, ipc); break;
        }
    }

    private _onRequestFromHtmlPathIndexHtml =  async (request: IpcRequest, ipc: Ipc) => {
        
    }

    private _registerCommonIpcOnMessageHandlerPathRoot(){
        this.registerCommonIpcOnMessageHandler({
            pathname: "/",
            matchMode: "full",
            input: {},
            output: "boolean",
            handler: async (args,client_ipc, request) => {
                return true;
            },
        });
        return this;
    }


    protected _shutdown(): unknown {
        throw new Error("Method not implemented.");
    }
  }
  