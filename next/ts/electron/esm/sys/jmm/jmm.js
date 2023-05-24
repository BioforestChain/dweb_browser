import { NativeMicroModule } from "../../core/micro-module.native.js";
import { log } from "../../helper/devtools.js";
import { JmmMetadata } from "./JmmMetadata.js";
import { JsMicroModule } from "./micro-module.js.js";
import { install, pause, resume, cancel } from "./jmm.handler.js";
import { createWWWServer } from "./jmm.www.serve.js";
import { createApiServer } from "./jmm.api.serve.js";
export class JmmNMM extends NativeMicroModule {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "mmid", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "jmm.sys.dweb"
        });
        Object.defineProperty(this, "downloadStatus", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: 0
        });
        Object.defineProperty(this, "wwwServer", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "apiServer", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "donwloadStramController", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "downloadStream", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "resume", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: {
                response: undefined,
                handler: async () => { },
            }
        });
        /**
         * 支持一次下载一个条目
         * 在下载完成之前多次点击， 会从头开始下载
         * @param req
         * @param response
         * @returns
         */
        // private _install = async(req: IncomingMessage, response: OutgoingMessage ) => {
        //   const origin = req.headers.origin || req.headers.referer;
        //   if(origin === undefined) throw new Error(`${this.mmid} _install origin === undefined`)
        //   const searchParams = new URL(req.url as string, `http://${this.mmid}/`).searchParams
        //   const metadataUrl = searchParams.get("metadataUrl")
        //   if(metadataUrl === null) return;
        //   this.downloadStatus = DOWNLOAD_STATUS.DOWNLOAD;
        //   this.resume = {
        //     response: response,
        //     handler: async () => {}
        //   }
        //   fetch(metadataUrl)
        //   .then(
        //     async(res) => {
        //       const data = await res.json();
        //       const downloadUrl = data.downloadUrl
        //       const tempPath = path.resolve(process.cwd(), `./temp/${data.title}.tar.gz`)
        //       const writeAblestream = fs.createWriteStream(tempPath, {flags: "w"});
        //             writeAblestream.on('close', this.onCloseAtWriteAblestream.bind(null, data, tempPath, response, origin))
        //       progress(request(downloadUrl), {})
        //       .on('progress', this.onProgress.bind(null, origin))
        //       .on('error', (err: Error) => {throw err})
        //       .pipe(writeAblestream)
        //     },
        //     err => {
        //       throw err
        //     }
        //   )
        // }
        // private onCloseAtWriteAblestream = async (
        //   data: $AppMetaData,
        //   tempPath: string,
        //   response: OutgoingMessage,
        //   origin: string
        // ) => {
        //   switch(this.downloadStatus){
        //     case DOWNLOAD_STATUS.DOWNLOAD:
        //       this._extract.bind(null, data,tempPath, response, origin)()
        //       break;
        //     case DOWNLOAD_STATUS.PAUSE:
        //       this.resume.handler = this._extract.bind(null, data,tempPath, response, origin)
        //       break;
        //     case DOWNLOAD_STATUS.CANCEL:
        //       break;
        //   }
        // }
        // onProgress = async (host: string, state: $State, installResponse: OutgoingMessage) => {
        //   switch(this.downloadStatus){
        //     case DOWNLOAD_STATUS.DOWNLOAD:
        //       const percent = (state.percent * 100).toFixed(0)
        //       this.changeProgress(percent, host)
        //       break;
        //     case DOWNLOAD_STATUS.PAUSE:
        //       break;
        //     case DOWNLOAD_STATUS.CANCEL:
        //       installResponse.writableEnded ? "" : installResponse.end(JSON.stringify({}))
        //       break;
        //   }
        // }
        // changeProgress = async (percent: string | number, host: string) => {
        // const url = `file://mwebview.sys.dweb/webview_execute_javascript_by_webview_url?`
        // const init: RequestInit = {
        //   body: `
        //     window.__app_upgrade_watcher_kit__._listeners.progress.forEach(callback => callback(${percent}));
        //     console.log('${percent}')
        //   `,
        //   method: "POST",
        //   headers: {
        //     "webview_url": host
        //   }
        // }
        // this.nativeFetch(url, init)
        // }
        // private _updateAppsInfo = async (data: $AppMetaData) => {
        //   // 读取文件内容
        //   const filename = path.resolve(__dirname, "../../../assets/data/apps_info.json")
        //   // 需要先检查是否有 如果没有就创建
        //   if(!fs.existsSync(filename)){
        //     await fsPromises.writeFile(filename, "")
        //   }
        //   const buffer = await fsPromises.readFile(filename)
        //   const content = buffer.length === 0 ? {} : JSON.parse(new TextDecoder().decode(buffer));
        //   const key = data.downloadUrl.split("/").pop()?.split(".")[0]
        //   if(key === undefined) throw new Error(`key === undefined`);
        //   content[key] = data;
        //   fsPromises.writeFile(filename, JSON.stringify(content))
        //   return this
        // }
        // /**
        //  * 暂停下载
        //  * @param req
        //  * @param response
        //  */
        // private _pause = async(req: IncomingMessage, response: OutgoingMessage ) => {
        //   // 只有下载状态才能够暂停
        //   if(this.downloadStatus === DOWNLOAD_STATUS.DOWNLOAD){
        //     this.downloadStatus = DOWNLOAD_STATUS.PAUSE
        //   }
        //   response.end()
        // }
        // /**
        //  * 重新下载
        //  * @param req
        //  * @param response
        //  */
        // private _resume = async(req: IncomingMessage, response: OutgoingMessage ) => {
        //   // 只有暂停状态才能够 重新下载
        //   if(this.downloadStatus === DOWNLOAD_STATUS.PAUSE){
        //     this.downloadStatus = DOWNLOAD_STATUS.DOWNLOAD
        //     this.resume.handler();
        //   }
        //   response.end()
        // }
        // /**
        //  * 取消下载
        //  * @param req
        //  * @param response
        //  */
        // private _cancel = async(req: IncomingMessage, response: OutgoingMessage ) => {
        //   if(this.downloadStatus === DOWNLOAD_STATUS.CANCEL) return;
        //   this.downloadStatus = DOWNLOAD_STATUS.CANCEL
        //   this.resume.response && !this.resume.response.writableEnded ? this.resume.response.end(JSON.stringify({})) : "";
        //   this.resume = {
        //     response: undefined,
        //     handler: async () => {}
        //   }
        //   response.end()
        // }
        // /**
        //  * 执行解压缩操作
        //  * @param data
        //  * @param tempPath
        //  * @param response
        //  * @param origin
        //  */
        // _extract = async (
        //   id: string,
        //   tempPath: string,
        //   request: IpcRequest,
        //   ipc: Ipc
        // ) => {
        //   const target = path.resolve(process.cwd(), `./apps/${id}`)
        //   tar.x(
        //     {
        //       cwd: target,
        //       file: tempPath,
        //       sync: true,
        //     }
        //   )
        //   await fsPromises.unlink(tempPath)
        //   // 需要把 json 文件保存起来
        //   // 同步的更新 json
        //   // this._updateAppsInfo(data)
        //   this.changeProgress(100, origin)
        //   // response.writableEnded ? "" : response.end(JSON.stringify({}));
        // }
    }
    async _bootstrap(context) {
        log.green(`[${this.mmid}] _bootstrap`);
        await createWWWServer.bind(this)();
        await createApiServer.bind(this)();
        //  安装 第三方 app
        this.registerCommonIpcOnMessageHandler({
            pathname: "/install",
            matchMode: "full",
            input: { metadataUrl: "string" },
            output: "boolean",
            handler: install.bind(this),
        });
        // 下载暂停
        this.registerCommonIpcOnMessageHandler({
            pathname: "/pause",
            matchMode: "full",
            input: {},
            output: "boolean",
            handler: pause.bind(this),
        });
        this.registerCommonIpcOnMessageHandler({
            pathname: "/resume",
            matchMode: "full",
            input: {},
            output: "boolean",
            handler: resume.bind(this),
        });
        this.registerCommonIpcOnMessageHandler({
            pathname: "/cancel",
            matchMode: "full",
            input: {},
            output: "boolean",
            handler: cancel.bind(this),
        });
        // this.registerCommonIpcOnMessageHandler({
        //   pathname: "/open_page",
        //   matchMode: "full",
        //   input: {},
        //   output: "object",
        //   handler: async (args, client_ipc, request) => {
        //     console.log('request: ', request)
        //     const path = require('path');
        //     const pathname = path.resolve(__dirname, './assets/index.html')
        //     console.log('pathname: ', pathname)
        //     const result = this.nativeFetch(`file:///assets/html/download.sys.dweb.html`)
        //     return result;
        //   }
        // })
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
    _shutdown() {
        throw new Error("Method not implemented.");
    }
    async openInstallPage(metadataUrl) {
        const config = await this.nativeFetch(metadataUrl).object();
        // TODO 打开安装页面
        /// 成功安装
        new JsMicroModule(new JmmMetadata(config));
        return config;
    }
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
export var DOWNLOAD_STATUS;
(function (DOWNLOAD_STATUS) {
    DOWNLOAD_STATUS[DOWNLOAD_STATUS["DOWNLOAD"] = 0] = "DOWNLOAD";
    DOWNLOAD_STATUS[DOWNLOAD_STATUS["PAUSE"] = 1] = "PAUSE";
    DOWNLOAD_STATUS[DOWNLOAD_STATUS["CANCEL"] = 2] = "CANCEL";
})(DOWNLOAD_STATUS || (DOWNLOAD_STATUS = {}));
