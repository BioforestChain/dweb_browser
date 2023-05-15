import type { IncomingMessage, OutgoingMessage } from "http";
import type { $BootstrapContext } from "../../core/bootstrapContext.cjs";
import type { HttpServerNMM } from "../http-server/http-server.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { log } from "../../helper/devtools.cjs";
import { $JmmMetadata, JmmMetadata } from "./JmmMetadata.cjs";
import { JsMicroModule } from "./micro-module.js.cjs";
const fs = require('fs');
const fsPromises = require('node:fs/promises')
const path = require('path')
const request = require('request');
const progress = require('request-progress');
const extract = require('extract-zip')
const tar = require('tar')

export class JmmNMM extends NativeMicroModule {
  mmid = "jmm.sys.dweb" as const;
  httpNMM: HttpServerNMM | undefined;
  downloadStatus: DOWNLOAD_STATUS = 0
  resume: {
    handler: Function,
    response: OutgoingMessage | undefined
  } = {
    response: undefined,
    handler: async () => {}
  }


  async _bootstrap(context: $BootstrapContext) {

    log.green(`[${this.mmid}] _bootstrap`)
     
    // this.httpNMM = (await context.dns.query('http.sys.dweb')) as HttpServerNMM
    // if(this.httpNMM === undefined) throw new Error(`[${this.mmid}] this.httpNMM === undefined`)

    // this.httpNMM.addRoute('/jmm.sys.dweb/install', this._install)
    // this.httpNMM.addRoute('/jmm.sys.dweb/pause', this._pause)
    // this.httpNMM.addRoute("/jmm.sys.dweb/resume", this._resume)
    // this.httpNMM.addRoute("/jmm.sys.dweb/cancel", this._cancel)


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

  /**
   * 支持一次下载一个条目
   * 在下载完成之前多次点击， 会从头开始下载
   * @param req 
   * @param response 
   * @returns 
   */
  private _install = async(req: IncomingMessage, response: OutgoingMessage ) => {
    const origin = req.headers.origin || req.headers.referer;
    if(origin === undefined) throw new Error(`${this.mmid} _install origin === undefined`)
    const searchParams = new URL(req.url as string, `http://${this.mmid}/`).searchParams
    const metadataUrl = searchParams.get("metadataUrl")
    if(metadataUrl === null) return;
    this.downloadStatus = DOWNLOAD_STATUS.DOWNLOAD;
    this.resume = {
      response: response,
      handler: async () => {}
    }
    fetch(metadataUrl)
    .then(
      async(res) => {
        const data = await res.json();
        const downloadUrl = data.downloadUrl
        const tempPath = path.resolve(process.cwd(), `./temp/${data.title}.tar.gz`)
        const writeAblestream = fs.createWriteStream(tempPath, {flags: "w"});
              writeAblestream.on('close', this.onCloseAtWriteAblestream.bind(null, data, tempPath, response, origin))
        progress(request(downloadUrl), {})
        .on('progress', this.onProgress.bind(null, origin))
        .on('error', (err: Error) => {throw err})
        .pipe(writeAblestream) 
      },
      err => {
        throw err
      }
    )
  }

  private onCloseAtWriteAblestream = async (
    data: $AppMetaData,
    tempPath: string,
    response: OutgoingMessage,
    origin: string
  ) => {

    switch(this.downloadStatus){
      case DOWNLOAD_STATUS.DOWNLOAD:
        this._extract.bind(null, data,tempPath, response, origin)()
        break;
      case DOWNLOAD_STATUS.PAUSE:
        this.resume.handler = this._extract.bind(null, data,tempPath, response, origin)
        break;
      case DOWNLOAD_STATUS.CANCEL:
        break;
    }
  }

  private onProgress = async (host: string, state: $State, installResponse: OutgoingMessage) => {
    switch(this.downloadStatus){
      case DOWNLOAD_STATUS.DOWNLOAD:
        const percent = (state.percent * 100).toFixed(0)
        this.changeProgress(percent, host)
        break;
      case DOWNLOAD_STATUS.PAUSE:
        break;
      case DOWNLOAD_STATUS.CANCEL:
        installResponse.writableEnded ? "" : installResponse.end(JSON.stringify({}))
        break;
    }
  }

  private changeProgress = async (percent: string | number, host: string) => {
    const url = `file://mwebview.sys.dweb/webview_execute_javascript_by_webview_url?`
    const init: RequestInit = {
      body: `
        window.__app_upgrade_watcher_kit__._listeners.progress.forEach(callback => callback(${percent}));
        console.log('${percent}')  
      `,
      method: "POST",
      headers: {
        "webview_url": host
      }
    }
    this.nativeFetch(url, init)
  }

  private _updateAppsInfo = async (data: $AppMetaData) => {
    // 读取文件内容
    const filename = path.resolve(__dirname, "../../../assets/data/apps_info.json")
    // 需要先检查是否有 如果没有就创建
    if(!fs.existsSync(filename)){
      await fsPromises.writeFile(filename, "")
    }
    const buffer = await fsPromises.readFile(filename)
    const content = buffer.length === 0 ? {} : JSON.parse(new TextDecoder().decode(buffer));
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
    // 只有下载状态才能够暂停
    if(this.downloadStatus === DOWNLOAD_STATUS.DOWNLOAD){
      this.downloadStatus = DOWNLOAD_STATUS.PAUSE
    }
    response.end()
  }

  /**
   * 重新下载
   * @param req 
   * @param response 
   */
  private _resume = async(req: IncomingMessage, response: OutgoingMessage ) => {
    // 只有暂停状态才能够 重新下载
    if(this.downloadStatus === DOWNLOAD_STATUS.PAUSE){
      this.downloadStatus = DOWNLOAD_STATUS.DOWNLOAD
      this.resume.handler();
    }
    response.end()
  }

  /**
   * 取消下载
   * @param req 
   * @param response 
   */
  private _cancel = async(req: IncomingMessage, response: OutgoingMessage ) => {
    if(this.downloadStatus === DOWNLOAD_STATUS.CANCEL) return;
    this.downloadStatus = DOWNLOAD_STATUS.CANCEL
    this.resume.response && !this.resume.response.writableEnded ? this.resume.response.end(JSON.stringify({})) : "";
    this.resume = {
      response: undefined,
      handler: async () => {}
    }
    response.end()
  }

  /**
   * 执行解压缩操作
   * @param data 
   * @param tempPath 
   * @param response 
   * @param origin 
   */
  private _extract = async (
    data: $AppMetaData,
    tempPath: string,
    response: OutgoingMessage,
    origin: string
  ) => {
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
    response.writableEnded ? "" : response.end(JSON.stringify({}));
  }
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

export enum DOWNLOAD_STATUS{
  DOWNLOAD,
  PAUSE,
  CANCEL
}