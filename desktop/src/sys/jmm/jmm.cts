//  jmm.sys.dweb 负责启动 第三方应用程序

import type { IncomingMessage, OutgoingMessage } from "http";
import type { $BootstrapContext } from "../../core/bootstrapContext.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import type { MicroModule } from "../../core/micro-module.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { log } from "../../helper/devtools.cjs";
import type { HttpServerNMM } from "../http-server/http-server.cjs";
import { $JmmMetadata, JmmMetadata } from "./JmmMetadata.cjs";
import { JsMicroModule } from "./micro-module.js.cjs";
const fs = require('fs');
const fsPromises = require('node:fs/promises')
const path = require('path')
const request = require('request');
const progress = require('request-progress');
const extract = require('extract-zip')

// 运行在 node 环境
export class JmmNMM extends NativeMicroModule {
  mmid = "jmm.sys.dweb" as const;
  httpNMM: HttpServerNMM | undefined;
  async _bootstrap(context: $BootstrapContext) {

    log.green(`[${this.mmid}] _bootstrap`)
    console.log("context.dns.query: ", context.dns.connect, context.dns.query)
    this.httpNMM = (await context.dns.query('http.sys.dweb')) as HttpServerNMM

    if(this.httpNMM === undefined) throw new Error(`[${this.mmid}] this.httpNMM === undefined`)

    this.httpNMM.addRoute('/jmm.sys.dweb/install', this._install)
    this.httpNMM.addRoute('/jmm.sys.dweb/pause', this._pause)
    this.httpNMM.addRoute("/jmm.sys.dweb/resume", this._resume)
    this.httpNMM.addRoute("/jmm.sys.dweb/cancel", this._cancel)


    // for (const app of this.apps.values()) {
    //   context.dns.install(app);
      
    // }
    // //  安装 第三方 app
    // this.registerCommonIpcOnMessageHandler({
    //   pathname: "/install",
    //   matchMode: "full",
    //   input: { metadataUrl: "string" },
    //   output: "boolean",
    //   handler: async (args, client_ipc, request) => {
    //     await this.openInstallPage(args.metadataUrl);
    //     return true;
    //   },
    // });

    // // 专门用来做静态服务
    // this.registerCommonIpcOnMessageHandler({
    //   pathname: "/open",
    //   matchMode: "full",
    //   input: {},
    //   output: "boolean",
    //   handler: async (args, client_ipc, request) => {
    //     const _url = new URL(request.url);
    //     let appId = _url.searchParams.get("appId");
    //     if (appId === null) return false;
    //     // 注意全部需要小写
    //     const mmid = createMMIDFromAppID(appId);
    //     const response = await this.nativeFetch(
    //       `file://dns.sys.dweb/open?app_id=${mmid}`
    //     );
    //     return IpcResponse.fromResponse(request.req_id, response, client_ipc);
    //   },
    // });
  }

  protected _shutdown(): unknown {
    throw new Error("Method not implemented.");
  }

  private async openInstallPage(metadataUrl: string) {
    const config = await this.nativeFetch(metadataUrl).object<$JmmMetadata>();
    // TODO 打开安装页面

    /// 成功安装
    new JsMicroModule(new JmmMetadata(config));
    return config;
  }

  private _install = async(req: IncomingMessage, response: OutgoingMessage ) => {
    const searchParams = new URL(req.url as string, `${req.headers.origin}`).searchParams
    const metadataUrl = searchParams.get("metadataUrl")
    if(metadataUrl === null) return;
    fetch(metadataUrl)
    .then(
      async(res) => {
        const data = await res.json();
        const downloadUrl = data.downloadUrl
        const tempPath = path.resolve(process.cwd(), `./temp/${data.title}.zip`)
        const writeAblestream = fs.createWriteStream(tempPath, {flags: "w"});

        writeAblestream.on('close', async () => {
          console.log('关闭了 解压还有问题')
          // // 解压
          // const target = path.resolve(process.cwd(), `./apps/${data.title}`)
          // console.log('target: ', target)
          // await extract(tempPath, { dir: target})
          // console.log('解压完成')
          // // 删除 tempPath
          // // await fsPromises.unlink(tempPath)
          // console.log('删除完成')
          // 下载完成返回
          response.end("")
          // console.log('返回完成')
        })
        progress(request(downloadUrl), {})
        .on('progress', (state:$State) => {console.log('state: ', state)})
        .on('error', (err: Error) => {throw err})
        .on('end', async() => {
          console.log('下载完毕')
        })
        .pipe(writeAblestream) 
        
      },
      err => {
        throw err
      }
    )
    console.log("metadataUrl: ", metadataUrl)

  }

  /**
   * 暂停下载
   * @param req 
   * @param response 
   */
  private _pause = async(req: IncomingMessage, response: OutgoingMessage ) => {

  }

  /**
   * 重新下载
   * @param req 
   * @param response 
   */
  private _resume = async(req: IncomingMessage, response: OutgoingMessage ) => {

  }

  /**
   * 取消下载
   * @param req 
   * @param response 
   */
  private _cancel = async(req: IncomingMessage, response: OutgoingMessage ) => {

  }
}



/**
 * 创建 mmid 根据appId
 */
function createMMIDFromAppID(appId: string) {
  return `app.${appId.toLocaleLowerCase()}.dweb` as $MMID;
}

export interface $State{
  percent: number             // Overall percent (between 0 to 1)
  speed: number               // The download speed in bytes/sec
  size: {
      total: number           // The total payload size in bytes
      transferred: number     // The transferred payload size in bytes
  },
  time: {
      elapsed: number,        // The total elapsed seconds since the start (3 decimals)
      remaining: number       // The remaining seconds to finish (3 decimals)
  }
}