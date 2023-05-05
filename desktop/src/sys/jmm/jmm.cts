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
const tar = require('tar')

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
    const origin = req.headers.origin;
    if(origin === undefined) throw new Error(`${this.mmid} _install origin === undefined`)
    const searchParams = new URL(req.url as string, `http://${this.mmid}/`).searchParams
    const metadataUrl = searchParams.get("metadataUrl")
    if(metadataUrl === null) return;
    fetch(metadataUrl)
    .then(
      async(res) => {
        const data = await res.json();
        const downloadUrl = data.downloadUrl
        const tempPath = path.resolve(process.cwd(), `./temp/${data.title}.tar.gz`)
        const writeAblestream = fs.createWriteStream(tempPath, {flags: "w"});

              writeAblestream.on('close', async () => {
                // 解压
                const target = path.resolve(process.cwd(), `./apps/${data.title}`)
                tar.x(
                  {
                    cwd: target,
                    file: tempPath,
                    sync: true,
                  }
                )
                await fsPromises.unlink(tempPath)
                // 需要把 json 文件保存起来
                // 同步的更新 json
                this._updateAppsInfo(data)
                this.changeProgress(100, origin)
                response.end("")
              })
        progress(request(downloadUrl), {})
        .on('progress', (state: $State) => this.onProgress(state,origin))
        .on('error', (err: Error) => {throw err})
        .pipe(writeAblestream) 
        
      },
      err => {
        throw err
      }
    )
    console.log("metadataUrl: ", metadataUrl)
  }

  private onProgress = async (state: $State, host: string) => {
    const percent = state.percent * 100
    this.changeProgress(percent, host)
  }

  private changeProgress = async (percent: string | number, host: string) => {
    const url = `file://mwebview.sys.dweb/webview_execute_javascript_by_host?`
    const init: RequestInit = {
      body: `window.__app_upgrade_watcher_kit__._listeners.progress.forEach(callback => callback(${percent}))`,
      method: "POST",
      headers: {
        "origin": host
      }
    }
    this.nativeFetch(url, init)
  }

  

  private _updateAppsInfo = async (data: $AppMetaData) => {
    // 读取文件内容
    // src/sys/jmm/jmm.cts
    const filename = path.resolve(__dirname, "../../../assets/data/apps_info.json")
    const buffer = await fsPromises.readFile(filename)
    const content = JSON.parse(new TextDecoder().decode(buffer))
    const key = data.downloadUrl.split("/").pop()?.split(".")[0]
    if(key === undefined) throw new Error(`key === undefined`);
    content[key] = data;
    fsPromises.writeFile(filename, JSON.stringify(content))
    return this
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

export interface $AppMetaData{
  title: string,
  subtitle: string,
  id: string,
  downloadUrl: string;
  icon: string,
  images: string[],
  introduction: string,
  author: string[],
  version: string,
  keywords: string[],
  home: string,
  mainUrl: string,
  server: { 
    root: string, 
    entry: string 
  },
  splashScreen: { entry: string},
  staticWebServers: $StaticWebServers[],
  openWebViewList: [],
  size: string,
  fileHash: '',
  permissions: string[],
  plugins: string[],
  releaseDate: string[]
}

export interface  $StaticWebServers{
  root: string;
  entry: string;
  subdomain: string;
  port: number;
}

// $AppMetaData 的实例 {
//   title: 'cotdemo',
//   subtitle: 'cotdemo',
//   id: 'cotdemo.bfs.dweb',
//   downloadUrl: 'https://shop.plaoc.com/KEJPMHLA/KEJPMHLA.bfsa',
//   icon: 'https://www.bfmeta.info/imgs/logo3.webp',
//   images: [
//     'http://qiniu-waterbang.waterbang.top/bfm/cot-home_2058.webp',
//     'http://qiniu-waterbang.waterbang.top/bfm/defi.png',
//     'http://qiniu-waterbang.waterbang.top/bfm/nft.png'
//   ],
//   introduction: 'The Super Mobile BlockChain for Metaverse',
//   author: [ 'bfs,bfs@bfs.com' ],
//   version: '1.1.3',
//   keywords: [ 'wallet' ],
//   home: 'https://www.bfmeta.info/',
//   mainUrl: '/assets/bfs.bfm.js',
//   server: { root: 'file:///bundle', entry: '/cotDemo.worker.js' },
//   splashScreen: { entry: 'https://www.bfmeta.org/' },
//   staticWebServers: [ { root: '', entry: 'index.html', subdomain: '', port: 80 } ],
//   openWebViewList: [],
//   size: '2342398472',
//   fileHash: '',
//   permissions: [ 'camera.sys.dweb', 'jmm.sys.dweb', '???.sys.dweb' ],
//   plugins: [ 'statusBar', 'share', 'notification', 'keyboard', 'scanner' ],
//   releaseDate: '1677667362583'
// }